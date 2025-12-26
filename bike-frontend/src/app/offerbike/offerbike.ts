import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit, OnDestroy, ChangeDetectorRef, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

/* ===== UI ALERT TYPE ===== */
interface AlertState {
  show: boolean;
  message: string;
  type: 'success' | 'error' | 'info';
}

@Component({
  selector: 'app-offerbike',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './offerbike.html',
  styleUrls: ['./offerbike.scss'],
})
export class OfferBikeComponent implements OnInit, OnDestroy {

  private _isLoading = false;

  get isLoading(): boolean {
    return this._isLoading;
  }

  set isLoading(value: boolean) {
    this._isLoading = value;
  }

  bike = {
    description: '',
    type: 'MOUNTAIN',
    rentalDailyRateEur: 0,
    offeredBy: ''
  };

  myOffers: any[] = [];
  private retryTimeout: any;

  // Custom Alert Signal
  alert = signal<AlertState>({ show: false, message: '', type: 'info' });

  constructor(
    private http: HttpClient, 
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.isLoading = false;
    this.tryLoadOffers();
  }

  ngOnDestroy(): void {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
    }
  }

  /* ===== UI HELPERS ===== */
  showAlert(message: string, type: 'success' | 'error' | 'info' = 'info') {
    this.alert.set({ show: true, message, type });
    setTimeout(() => this.alert.set({ ...this.alert(), show: false }), 5000);
  }

  private tryLoadOffers() {
    this.extractUser();

    const token = localStorage.getItem('token');

    if (!this.bike.offeredBy || !token) {
      this.retryTimeout = setTimeout(() => this.tryLoadOffers(), 1000);
      return;
    }

    this.loadMyOffers();
  }

  private extractUser() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.bike.offeredBy = payload.sub;
      } catch (e) {
        console.error('Error decoding token:', e);
      }
    }

    if (!this.bike.offeredBy) {
      const userStr = localStorage.getItem('user');
      if (userStr) {
        const userObj = JSON.parse(userStr);
        this.bike.offeredBy = userObj.id || userObj.sub;
      }
    }
  }

  private loadMyOffers() {
    if (!this.bike.offeredBy) {
      console.warn('Cannot load offers: no offeredBy set.');
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      console.error('Cannot load offers: no token found.');
      return;
    }

    this.isLoading = true;

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.http.get<any[]>(`http://localhost:8080/api/bikes?offeredById=${this.bike.offeredBy}`, { headers })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (data) => {
          this.myOffers = data || [];
        },
        error: (err) => {
          console.error('Error loading offers:', err);
          this.myOffers = [];
        }
      });
  }

  submitOffer() {
    if (!this.bike.offeredBy) return;

    this.isLoading = true;

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    const payload = {
      description: this.bike.description,
      rentalDailyRateEur: this.bike.rentalDailyRateEur,
      offeredById: this.bike.offeredBy,
      offeredByType: 'STUDENT',
      type: this.bike.type
    };

    this.http.post('http://localhost:8080/api/bikes', payload, { headers })
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        })
      )
      .subscribe({
        next: (response: any) => {
          this.showAlert('Bike listed successfully!', 'success');

          const newBikeVisual = {
            ...payload,
            status: 'AVAILABLE',
            id: response?.id || Date.now()
          };
          this.myOffers.unshift(newBikeVisual);

          this.bike.description = '';
          this.bike.rentalDailyRateEur = 0;
        },
        error: (error) => {
          console.error('Error submitting offer:', error);
          const errorMsg = error.error?.detail || error.message || "Check connection";
          this.showAlert('Failed: ' + errorMsg, 'error');
        }
      });
  }
selectedBikeNotes = signal<any[]>([]);
showNotesModal = signal(false);
viewBikeHistory(bikeId: number) {
  this.http.get<any[]>(`http://localhost:8080/api/bikes/${bikeId}/return-notes`, { 
    headers: new HttpHeaders().set('Authorization', `Bearer ${localStorage.getItem('token')}`) 
  }).subscribe({
    next: (notes) => {
      this.selectedBikeNotes.set(notes);
      this.showNotesModal.set(true);
    },
    error: (err) => this.showAlert('Could not load history', 'error')
  });
}

}