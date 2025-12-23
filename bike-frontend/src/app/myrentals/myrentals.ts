import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

/* ===== DTO TYPES (MATCH BACKEND) ===== */
interface ActiveRentalDto {
  rentalId: number;
  message: string;          // Bike name
  result: 'RENTED' | 'ACTIVE';
  waitingListEntryId: number | null;
}

interface WaitlistDto {
  bikeId: number;
  createdAt: string;
  servedAt?: string | null;
}

interface AlertState {
  show: boolean;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-myrentals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './myrentals.html',
  styleUrl: './myrentals.scss',
})
export class Myrentals implements OnInit {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';

  /* ===== STATE ===== */
  allRentalsRaw = signal<ActiveRentalDto[]>([]);
  allWaitlistRaw = signal<WaitlistDto[]>([]);
  searchQuery = signal('');
  
  // Custom Alert Signal
  alert = signal<AlertState>({ show: false, message: '', type: 'info' });

  /* ===== COMPUTED ===== */
  myActiveRentals = computed(() => {
    const query = this.searchQuery().toLowerCase();
    return this.allRentalsRaw().filter(r =>
      r.result === 'RENTED' &&
      (
        r.rentalId.toString().includes(query) ||
        r.message.toLowerCase().includes(query)
      )
    );
  });

  myWaitlist = computed(() => {
    return this.allWaitlistRaw().filter(w => !w.servedAt);
  });

  /* ===== FORM ===== */
  returnForm = {
    rentalId: null as number | null,
    condition: 'GOOD',
    comment: ''
  };

  /* ===== LIFECYCLE ===== */
  ngOnInit() {
    this.refreshData();
  }

  /* ===== UI HELPERS ===== */
  showAlert(message: string, type: 'success' | 'error' | 'info' = 'info') {
    this.alert.set({ show: true, message, type });
    setTimeout(() => this.alert.set({ ...this.alert(), show: false }), 5000);
  }

  /* ===== DATA LOADING ===== */
  refreshData() {
    const userId = this.getUserId();
    if (!userId) return;

    const headers = this.getHeaders();

    this.http
      .get<ActiveRentalDto[]>(`${this.baseUrl}/rentals/active?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allRentalsRaw.set(data),
        error: (err) => console.error('[MyRentals] Active rentals error:', err),
      });

    this.http
      .get<WaitlistDto[]>(`${this.baseUrl}/rentals/waitlist?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allWaitlistRaw.set(data),
        error: (err) => console.error('[MyRentals] Waitlist error:', err),
      });
  }

  /* ===== ACTIONS ===== */
  returnBike() {
    const userId = this.getUserId();
    
    if (!this.returnForm.rentalId) {
      this.showAlert('Please select a rental from the list first.', 'info');
      return;
    }

    if (!userId) {
      this.showAlert('User session expired. Please log in again.', 'error');
      return;
    }

    const body = {
      authorCustomerId: userId, 
      comment: this.returnForm.comment,
      condition: this.returnForm.condition,
    };

    this.http
      .post(`${this.baseUrl}/rentals/${this.returnForm.rentalId}/return`, body, { headers: this.getHeaders() })
      .subscribe({
        next: () => {
          this.showAlert('Bike returned successfully! The next user has been notified.', 'success');
          this.returnForm = { rentalId: null, condition: 'GOOD', comment: '' };
          this.refreshData(); 
        },
        error: (err) => {
          this.showAlert('Return failed. Please check your comments and try again.', 'error');
          console.error('Return error:', err);
        },
      });
  }

  prefillReturn(rentalId: number) {
    this.returnForm.rentalId = rentalId;
    const formElement = document.querySelector('form');
    formElement?.scrollIntoView({ behavior: 'smooth' });
  }

  /* ===== HELPERS ===== */
  private getHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  private getUserId(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub;
    } catch {
      return null;
    }
  }
}