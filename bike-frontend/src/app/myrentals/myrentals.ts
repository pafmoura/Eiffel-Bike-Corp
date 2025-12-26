import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface ActiveRentalDto {
  rentalId: number;
  message: string;      // Bike name/description
  result: 'RENTED' | 'ACTIVE';
  waitingListEntryId: number | null;
}

interface WaitlistDto {
  id: number;          
  customerId: string;  
  bikeId: number;
  message: string;     
  sentAt: string;      
  servedAt?: string | null;
}

interface RentalResponse {
  id: number;
  bikeId: number;
  customerId: string;
  status: string;
  startAt: string;
  endAt: string | null;
  totalAmountEur: number;
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

  /* ===== STATE SIGNALS ===== */
  allRentalsRaw = signal<ActiveRentalDto[]>([]);
  allWaitlistRaw = signal<WaitlistDto[]>([]);
  allHistoryRaw = signal<RentalResponse[]>([]); 
  searchQuery = signal('');
  alert = signal<AlertState>({ show: false, message: '', type: 'info' });
  
  // Controls the History Dialog visibility
  isHistoryOpen = signal(false);

  /* ===== COMPUTED PROPERTIES ===== */
  myActiveRentals = computed(() => {
    const query = this.searchQuery().toLowerCase();
    return this.allRentalsRaw().filter(r =>
      (r.result === 'RENTED' || r.result === 'ACTIVE') &&
      (r.rentalId.toString().includes(query) || r.message.toLowerCase().includes(query))
    );
  });

  myWaitlist = computed(() => {
    return this.allWaitlistRaw().filter(w => !w.servedAt);
  });

  /** US_21: Computed history filtered by search and sorted by newest first */
  myHistory = computed(() => {
    const query = this.searchQuery().toLowerCase();
    return this.allHistoryRaw()
      .filter(h => 
        h.id.toString().includes(query) || 
        h.status.toLowerCase().includes(query)
      )
      .sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());
  });

  /* ===== FORM STATE ===== */
  returnForm = {
    rentalId: null as number | null,
    condition: 'GOOD',
    comment: ''
  };

  /* ===== LIFECYCLE ===== */
  ngOnInit() {
    this.refreshDashboard();
    this.loadRentalHistory(); 
  }

  /* ===== DATA LOADING ===== */
  
  /** Refreshes active components (Rentals and Waitlists) */
  refreshDashboard() {
    const userId = this.getUserId();
    if (!userId) return;

    const headers = this.getHeaders();

    this.http
      .get<ActiveRentalDto[]>(`${this.baseUrl}/rentals/active?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allRentalsRaw.set(data),
        error: (err) => console.error('[MyRentals] Active error:', err),
      });

    this.http
      .get<WaitlistDto[]>(`${this.baseUrl}/rentals/waitlist?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allWaitlistRaw.set(data),
        error: (err) => console.error('[MyRentals] Waitlist error:', err),
      });
  }

  /** US_21: Fetch full rental history from GET /api/rentals */
  loadRentalHistory() {
    const headers = this.getHeaders();
    this.http
      .get<RentalResponse[]>(`${this.baseUrl}/rentals`, { headers })
      .subscribe({
        next: (data) => this.allHistoryRaw.set(data),
        error: (err) => console.error('[MyRentals] History error:', err),
      });
  }

  /* ===== ACTIONS ===== */
  returnBike() {
    const userId = this.getUserId();
    if (!this.returnForm.rentalId || !userId) {
      this.showAlert('Validation failed. Please select a rental.', 'error');
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
          this.showAlert('Bike returned successfully!', 'success');
          this.returnForm = { rentalId: null, condition: 'GOOD', comment: '' };
          // Refresh everything
          this.refreshDashboard(); 
          this.loadRentalHistory();
        },
        error: (err) => {
          this.showAlert('Return failed. Please try again.', 'error');
          console.error('Return error:', err);
        },
      });
  }

  prefillReturn(rentalId: number) {
    this.returnForm.rentalId = rentalId;
    document.querySelector('form')?.scrollIntoView({ behavior: 'smooth' });
  }

  showAlert(message: string, type: 'success' | 'error' | 'info' = 'info') {
    this.alert.set({ show: true, message, type });
    setTimeout(() => this.alert.set({ ...this.alert(), show: false }), 5000);
  }

  /* ===== HELPERS ===== */
  calculateTotalSpent(): number {
    return this.allHistoryRaw().reduce((sum, item) => sum + (item.totalAmountEur || 0), 0);
  }

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