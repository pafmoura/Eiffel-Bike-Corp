import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';
  
  // State
  bikes = signal<any[]>([]);
  isPaymentStep = signal(false);
  selectedBike = signal<any>(null);
  
  // Payment Form Fields
  cardNumber = signal('');
  expiry = signal('');
  cvc = signal('');
  currency = signal('EUR');
  currencies = signal(['EUR', 'USD', 'GBP']);

  readonly bikeImages: string[] = [
    'https://cdn.shopify.com/s/files/1/0290/9382/2538/files/emtb29fsweb_f504cf91-1a89-499d-aa9a-298835327cd3.jpg?width=800&crop=center',
    'https://c02.purpledshub.com/uploads/sites/39/2023/04/Giant-Propel-Advanced-Pro-0-AXS-01-edbd219.jpg?webp=1&w=1200',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQuqnSeZPqSaCHmM8Wj8fqD8bPkHKd1HODlCw&s'
  ];

  ngOnInit() { 
    this.loadBikes(); 
  }

  private getHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  loadBikes() {
    this.http.get<any[]>(`${this.baseUrl}/bikes/all`, { headers: this.getHeaders() }).subscribe({
      next: (data) => {
        this.bikes.set(data.map(bike => ({
          ...bike, 
          imageUrl: this.bikeImages[Math.floor(Math.random() * this.bikeImages.length)]
        })));
      },
      error: () => console.error('Failed to load bikes.')
    });
  }

  handleRentClick(bike: any) {
    const token = localStorage.getItem('token');
    if (!token) { 
      alert('Please log in first'); 
      return; 
    }

    if (bike.status !== 'AVAILABLE') {
      this.createRental(bike, false);
      return;
    }

    this.selectedBike.set(bike);
    this.isPaymentStep.set(true);
  }

  confirmPaymentAndRent() {
    const bike = this.selectedBike();
    this.createRental(bike, true);
  }

  private createRental(bike: any, proceedToPayment: boolean) {
    const token = localStorage.getItem('token');
    const payload = JSON.parse(atob(token!.split('.')[1]));

    const rentalRequest = { 
      bikeId: bike.id, 
      customerId: payload.sub, 
      days: 3 
    };

    this.http.post<any>(`${this.baseUrl}/rentals`, rentalRequest, { headers: this.getHeaders() })
      .subscribe({
        next: (res) => {
          // res is RentBikeResultResponse: { result, rentalId, waitingListEntryId, message }
          if (proceedToPayment && res.rentalId) {
            this.processPayment(res.rentalId, bike);
          } else {
            alert(res.message || 'Added to Waiting List.');
            this.closeModal();
          }
        },
        error: (err) => {
          console.error('Rental creation failed:', err);
          alert('Could not process rental request.');
        }
      });
  }

  private processPayment(rentalId: number, bike: any) {
    const totalAmount = parseFloat((bike.rentalDailyRateEur * 3).toFixed(2));

    const paymentData = {
      rentalId: Number(rentalId),
      amount: totalAmount,
      currency: this.currency().toUpperCase(),
      paymentMethodId: 'pm_card_visa'
    };

    this.http.post(`${this.baseUrl}/payments/rentals`, paymentData, { headers: this.getHeaders() })
      .subscribe({
        next: () => {
          alert('Success! Payment confirmed and bike rented.');
          this.closeModal();
        },
        error: (err) => {
          console.error('Payment failed:', err.error);
          alert('Payment refused. The rental was created but remains unpaid.');
          this.closeModal();
        }
      });
  }

  closeModal() {
    this.isPaymentStep.set(false);
    this.selectedBike.set(null);
    this.cardNumber.set('');
    this.loadBikes();
  }

  convert(value: number) {
    const rates: any = { 'EUR': 1, 'USD': 1.1, 'GBP': 0.85 };
    return (value || 0) * (rates[this.currency()] || 1);
  }
}