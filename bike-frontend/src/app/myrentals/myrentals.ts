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

  /* ===== DATA LOADING ===== */
  refreshData() {
    const userId = this.getUserId();
    console.log('[MyRentals] refreshData() userId:', userId);

    if (!userId) return;

    const headers = this.getHeaders();

    this.http
      .get<ActiveRentalDto[]>(`${this.baseUrl}/rentals/active?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => {
          console.log('[MyRentals] Active rentals:', data);
          this.allRentalsRaw.set(data);
        },
        error: (err) =>
          console.error('[MyRentals] Active rentals error:', err),
      });

    this.http
      .get<WaitlistDto[]>(`${this.baseUrl}/rentals/waitlist?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => {
          console.log('[MyRentals] Waitlist:', data);
          this.allWaitlistRaw.set(data);
        },
        error: (err) =>
          console.error('[MyRentals] Waitlist error:', err),
      });
  }

/* ===== ACTIONS ===== */
  returnBike() {
    const userId = this.getUserId(); // This returns the UUID from the token
    
    if (!this.returnForm.rentalId) {
      alert('Please select a rental first.');
      return;
    }

    if (!userId) {
      alert('User session expired. Please log in again.');
      return;
    }

    const body = {
      authorCustomerId: userId, 
      comment: this.returnForm.comment,
      condition: this.returnForm.condition,
    };

    this.http
      .post(
        `${this.baseUrl}/rentals/${this.returnForm.rentalId}/return`,
        body,
        { headers: this.getHeaders() }
      )
      .subscribe({
        next: () => {
          alert('Bike returned successfully! ');
          this.returnForm = { rentalId: null, condition: 'GOOD', comment: '' };
          this.refreshData(); 
        },
        error: (err) => {
          console.error('Return error:', err);
          alert('Return failed. Ensure all fields are filled correctly.');
        },
      });
  }
  prefillReturn(rentalId: number) {
    this.returnForm.rentalId = rentalId;
    document.querySelector('form')?.scrollIntoView({ behavior: 'smooth' });
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
