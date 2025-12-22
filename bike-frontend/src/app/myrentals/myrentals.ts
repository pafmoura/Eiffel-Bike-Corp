import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

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

  // --- SIGNALS (Reactive State) ---
  // We store the "raw" data from the server here
  allRentalsRaw = signal<any[]>([]);
  allWaitlistRaw = signal<any[]>([]);
  
  // UI Search/Filter state
  searchQuery = signal<string>('');

  // --- COMPUTED SIGNALS (Auto-filtering) ---
  // These update automatically whenever raw data or the search query changes
  myActiveRentals = computed(() => {
    const userId = this.getUserId();
    return this.allRentalsRaw().filter(r => 
      r.customerId === userId && 
      (r.status === 'ACTIVE' || r.status === 'RENTED') &&
      (r.id.toString().includes(this.searchQuery()) || 
       r.bikeName?.toLowerCase().includes(this.searchQuery().toLowerCase()))
    );
  });

  myWaitlist = computed(() => {
    const userId = this.getUserId();
    return this.allWaitlistRaw().filter(w => 
      w.customerId === userId && 
      !w.servedAt // Only show those not yet served
    );
  });

  // --- FORM STATE ---
  returnForm = {
    rentalId: null as number | null,
    condition: 'GOOD',
    comment: ''
  };

  ngOnInit() {
    this.refreshData();
  }

  // --- DATA LOADING ---
  refreshData() {
    const userId = this.getUserId();
    if (!userId) return;

    const headers = this.getHeaders();

    // Strategy: Fetch from the existing endpoints and filter locally
    // If your backend has a 'GET /rentals' that returns all, use that.
    // Otherwise, we use the specific ones you might have.
    this.http.get<any[]>(`${this.baseUrl}/rentals/active?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allRentalsRaw.set(data),
        error: (err) => console.error('Error fetching rentals:', err)
      });

    this.http.get<any[]>(`${this.baseUrl}/rentals/waitlist?customerId=${userId}`, { headers })
      .subscribe({
        next: (data) => this.allWaitlistRaw.set(data),
        error: (err) => console.error('Error fetching waitlist:', err)
      });
  }

  // --- ACTIONS ---
  returnBike() {
    const userId = this.getUserId();
    if (!this.returnForm.rentalId || !userId) {
      alert('Please select a bike to return.');
      return;
    }

    const body = {
      authorCustomerId: userId,
      comment: this.returnForm.comment,
      condition: this.returnForm.condition
    };

    this.http.post(`${this.baseUrl}/rentals/${this.returnForm.rentalId}/return`, body, { headers: this.getHeaders() })
      .subscribe({
        next: () => {
          alert('Bike returned successfully!');
          this.returnForm = { rentalId: null, condition: 'GOOD', comment: '' };
          this.refreshData(); // Reload lists
        },
        error: (err) => alert('Return failed. Ensure the Rental ID is correct.')
      });
  }

  prefillReturn(rentalId: number) {
    this.returnForm.rentalId = rentalId;
    // Optional: smooth scroll to form
    document.querySelector('form')?.scrollIntoView({ behavior: 'smooth' });
  }

  // --- HELPERS ---
  private getHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  private getUserId(): string | null {
    const token = localStorage.getItem('token');
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub; // This is your UUID
    } catch (e) {
      return null;
    }
  }
}