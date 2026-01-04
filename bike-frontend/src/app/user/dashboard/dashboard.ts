import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { FxRateService } from '../../fx-rate-service';
import { UserService } from '../../services/user-service';



interface AlertState {
  show: boolean;
  message: string;
  type: 'success' | 'error' | 'info';
}
/**
 * Dashboard component for user interactions.
 * Displays available bikes, manages rentals, and handles payments.
 */
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
  private userService = inject(UserService);

  alert = signal<AlertState>({ show: false, message: '', type: 'info' });
  bikes = signal<any[]>([]);
  myActiveRentals = signal<any[]>([]);

  isPaymentStep = signal(false);
  selectedBike = signal<any>(null);
  rentalDays = signal(3);

  userId = signal<string | null>(null);

  cardNumber = signal('');
  expiry = signal('');
  cvc = signal('');

  readonly bikeImages = [
    'https://cdn.shopify.com/s/files/1/0290/9382/2538/files/emtb29fsweb_f504cf91-1a89-499d-aa9a-298835327cd3.jpg?width=800&crop=center',
    'https://c02.purpledshub.com/uploads/sites/39/2023/04/Giant-Propel-Advanced-Pro-0-AXS-01-edbd219.jpg?webp=1&w=1200',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQuqnSeZPqSaCHmM8Wj8fqD8bPkHKd1HODlCw&s'
  ];

  constructor() {
    effect(() => {
      const user = this.userService.currentUser();
      if (!user) return;
      
      if (this.userService.userReady()){
        this.userId.set(user.id); 
        this.loadMyRentalsAndBikes();
        this.loadNotifications();
      }
    });
  }

  ngOnInit() {}

  notifications = signal<any[]>([]); 

  /**
   * Displays user notifications related to rentals.
   */
  loadNotifications() {
    const id = this.userId();
    if (!id) return;

    this.http.get<any[]>(`${this.baseUrl}/rentals/notifications?customerId=${id}`, { 
      headers: this.getHeaders() 
    }).subscribe({
      next: (data) => this.notifications.set(data),
      error: (err) => console.error('Could not load notifications', err)
    });
  }

  showAlert(message: string, type: 'success' | 'error' | 'info' = 'info') {
    this.alert.set({ show: true, message, type });
    setTimeout(() => this.alert.set({ ...this.alert(), show: false }), 5000);
  }

  private getHeaders() {
    return new HttpHeaders().set('Authorization', `Bearer ${localStorage.getItem('token')}`);
  }

  /**
   * Loads the user's active rentals and all bikes, marking which are rented or reserved by the user.
   */
  loadMyRentalsAndBikes() {
    // 1. Get all rentals for this user to check for RESERVED status
    this.http.get<any[]>(
      `${this.baseUrl}/rentals`,
      { headers: this.getHeaders() }
    ).subscribe(allRentals => {
      
      // Map out active bikes and specific reserved rentals
      const activeBikeIds = new Set(allRentals.filter(r => r.status === 'ACTIVE').map(r => r.bikeId));
      const reservedMap = new Map(allRentals.filter(r => r.status === 'RESERVED').map(r => [r.bikeId, r.id]));

      // 2. Get the full bike catalog
      this.http.get<any[]>(
        `${this.baseUrl}/bikes/all`,
        { headers: this.getHeaders() }
      ).subscribe(bikes => {
        const mapped = bikes.map(b => ({
          ...b,
          imageUrl: this.bikeImages[Math.floor(Math.random() * this.bikeImages.length)],
          isRentedByMe: activeBikeIds.has(b.id),
          isReservedForMe: reservedMap.has(b.id),
          reservedRentalId: reservedMap.get(b.id)
        }));

        this.bikes.set(mapped);
      });
    });
  }

  /**
   * Handles the rent button click for a bike.
   */
  handleRentClick(bike: any) {
    if (bike.isRentedByMe) return;

    if (bike.offeredBy.id === this.userId()) {
this.showAlert("You can't rent your own bike.", 'error');
      return;
    }

    // CASE 1: Bike is reserved for me (from waitlist) -> Go straight to payment
    if (bike.isReservedForMe) {
      this.selectedBike.set(bike);
      this.isPaymentStep.set(true);
      return;
    }

    // CASE 2: Bike is available -> Proceed to payment step
    if (bike.status === 'AVAILABLE') {
      this.selectedBike.set(bike);
      this.isPaymentStep.set(true);
      return;
    }

    // CASE 3: Bike is taken -> Join waitlist (no payment yet)
    this.createRental(bike, false);
  }

  /**
   * Confirms payment.
   */
  confirmPaymentAndRent() {
    const bike = this.selectedBike();
    
    // If it was already reserved from a waitlist, use the existing rental ID
    if (bike.isReservedForMe && bike.reservedRentalId) {
      this.processPayment(bike.reservedRentalId, bike);
    } else {
      // Otherwise, create a new rental and pay
      this.createRental(bike, true);
    }
  }

  private createRental(bike: any, pay: boolean) {
    const rentalRequest = {
      bikeId: bike.id,
      customerId: this.userId(),
      days: this.rentalDays()
    };

    this.http.post<any>(
      `${this.baseUrl}/rentals`,
      rentalRequest,
      { headers: this.getHeaders() }
    ).subscribe(res => {
      // res.rentalId is returned if status is RENTED (Active) or RESERVED
      if (pay && res.rentalId) {
        this.processPayment(res.rentalId, bike);
      } else {
        this.showAlert(res.message || 'Action completed.', 'success');
        this.closeModal();
      }
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

    this.http.post(
      `${this.baseUrl}/payments/rentals`,
      paymentData,
      { headers: this.getHeaders() }
    ).subscribe({
      next: () => {
this.showAlert('Payment successful! Your rental is now active.', 'success');
        this.closeModal();
      },
      error: (err) => {
        console.error(err);
        this.showAlert('Payment failed. Please try again.', 'error');
      }
    });
  }

  closeModal() {
    this.isPaymentStep.set(false);
    this.selectedBike.set(null);
    this.rentalDays.set(3);
    this.loadMyRentalsAndBikes(); 
    this.loadNotifications();
  }
}