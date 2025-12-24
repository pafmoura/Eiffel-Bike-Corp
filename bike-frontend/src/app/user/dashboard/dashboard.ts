import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FxRateService } from '../../fx-rate-service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {

  private http = inject(HttpClient);
  public fx = inject(FxRateService);

  private baseUrl = 'http://localhost:8080/api';

  bikes = signal<any[]>([]);
  isPaymentStep = signal(false);
  selectedBike = signal<any>(null);
  rentalDays = signal(3);

  userId = signal<number | null>(null);

  cardNumber = signal('');
  expiry = signal('');
  cvc = signal('');

  readonly bikeImages = [
    'https://cdn.shopify.com/s/files/1/0290/9382/2538/files/emtb29fsweb_f504cf91-1a89-499d-aa9a-298835327cd3.jpg?width=800&crop=center',
    'https://c02.purpledshub.com/uploads/sites/39/2023/04/Giant-Propel-Advanced-Pro-0-AXS-01-edbd219.jpg?webp=1&w=1200',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQuqnSeZPqSaCHmM8Wj8fqD8bPkHKd1HODlCw&s'
  ];

  ngOnInit() {
    const token = localStorage.getItem('token');
    if (token) {
  const payload = JSON.parse(atob(token.split('.')[1]));
    this.userId.set(payload.sub);    }
    this.loadBikes();
  }

  private getHeaders() {
    return new HttpHeaders().set('Authorization', `Bearer ${localStorage.getItem('token')}`);
  }

  loadBikes() {
  this.http.get<any[]>(`${this.baseUrl}/bikes/all`, { headers: this.getHeaders() })
    .subscribe(data => {
      const mapped = data.map(b => ({
        ...b,
        imageUrl: this.bikeImages[Math.floor(Math.random() * this.bikeImages.length)]
      }));

      this.bikes.set(mapped);

      console.log('Bikes loaded:', this.bikes());
    });
}

handleRentClick(bike: any) {
  if (bike.offeredBy.id === this.userId()) {
    alert("You can't rent your own bike.");
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
    this.createRental(this.selectedBike(), true);
  }

  private createRental(bike: any, pay: boolean) {
    const rentalRequest = {
      bikeId: bike.id,
      customerId: this.userId(),
      days: this.rentalDays()
    };

    this.http.post<any>(`${this.baseUrl}/rentals`, rentalRequest, { headers: this.getHeaders() }).subscribe(res => {
      if (pay && res.rentalId) this.processPayment(res.rentalId, bike);
      else alert(res.message || 'Added to Waiting List.');
      this.closeModal();
    });
  }

  increaseDays() { if (this.rentalDays() < 30) this.rentalDays.update(v => v + 1); }
  decreaseDays() { if (this.rentalDays() > 1) this.rentalDays.update(v => v - 1); }

  private processPayment(rentalId: number, bike: any) {
    const paymentData = {
      rentalId,
      amount: +(bike.rentalDailyRateEur * this.rentalDays()).toFixed(2),
      currency: 'EUR',
      paymentMethodId: 'pm_card_visa'
    };

    this.http.post(`${this.baseUrl}/payments/rentals`, paymentData, { headers: this.getHeaders() }).subscribe(() => {
      alert('Payment successful!');
      this.closeModal();
    });
  }

  closeModal() {
    this.isPaymentStep.set(false);
    this.selectedBike.set(null);
    this.rentalDays.set(3);
    this.loadBikes();
  }
}
