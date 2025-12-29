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
  // --- UI State Signals ---
  showSaleModal = signal(false);
  showNotesModal = signal(false);
  selectedBikeForSale = signal<any>(null);
  selectedBikeNotes = signal<any[]>([]);
  alert = signal<AlertState>({ show: false, message: '', type: 'info' });

  // --- Form Variables (Standard variables work best with ngModel) ---
  salePrice: number = 0;
  saleNote: string = '';
  
  bike = {
    description: '',
    type: 'MOUNTAIN',
    rentalDailyRateEur: 0,
    offeredBy: ''
  };

  // --- Logic State ---
  myOffers: any[] = [];
  private _isLoading = false;
  private retryTimeout: any;

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

  /* =========================================================
     SALE OFFERS LOGIC (The part causing the 404/401)
     ========================================================= */

  openSaleModal(bike: any) {
    this.selectedBikeForSale.set(bike);
    // Suggest a default price (e.g., 10x the daily rental rate)
    this.salePrice = bike.rentalDailyRateEur * 10;
    this.saleNote = '';
    this.showSaleModal.set(true);
  }

  submitSaleOffer() {
    const bike = this.selectedBikeForSale();
    if (!bike || this.salePrice <= 0) {
      this.showAlert('Please enter a valid price', 'error');
      return;
    }

    this.isLoading = true;

    const salePayload = {
      bikeId: bike.id,
      sellerId: this.bike.offeredBy, 
      askingPriceEur: this.salePrice
    };

    console.log('Submitting sale offer:', salePayload);

    this.http.post('http://localhost:8080/api/sale-offers', salePayload)
      .subscribe({
        next: (saleResponse: any) => {
          if (this.saleNote.trim()) {
            this.addInitialSaleNote(saleResponse.id);
          } else {
            this.finalizeSaleSuccess();
          }
        },
        error: (err) => {
          this.isLoading = false;
          console.error('Sale error:', err);
          const errorDetail = err.error?.detail || 'Unauthorized or Endpoint not found (404/401)';
          this.showAlert(errorDetail, 'error');
        }
      });
  }

  private addInitialSaleNote(saleOfferId: number) {
    const notePayload = {
      saleOfferId: saleOfferId,
      content: this.saleNote
    };

    this.http.post('http://localhost:8080/api/sale-offers/notes', notePayload)
      .pipe(finalize(() => this.finalizeSaleSuccess()))
      .subscribe({
        error: (err) => {
          console.error('Note error:', err);
          this.showAlert('Offer created, but condition note failed to save.', 'info');
        }
      });
  }

  private finalizeSaleSuccess() {
    this.isLoading = false;
    this.showSaleModal.set(false);
    this.showAlert('Bike is now listed for sale!', 'success');
    this.loadMyOffers(); 
  }

  submitOffer() {
    if (!this.bike.offeredBy) return;

    this.isLoading = true;
    const payload = {
      description: this.bike.description,
      rentalDailyRateEur: this.bike.rentalDailyRateEur,
      offeredById: this.bike.offeredBy,
      offeredByType: 'STUDENT',
      type: this.bike.type
    };

    this.http.post('http://localhost:8080/api/rental-offers', payload)
      .pipe(finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (response: any) => {
          this.showAlert('Bike listed successfully for rent!', 'success');
          
          // Optimistically update UI
          const newBikeVisual = {
            ...payload,
            status: 'AVAILABLE',
            id: response?.id || Date.now()
          };
          this.myOffers.unshift(newBikeVisual);

          // Reset form
          this.bike.description = '';
          this.bike.rentalDailyRateEur = 0;
        },
        error: (error) => {
          const errorMsg = error.error?.detail || "Check your connection or permissions";
          this.showAlert('Failed: ' + errorMsg, 'error');
        }
      });
  }

  viewBikeHistory(bikeId: number) {
    this.http.get<any[]>(`http://localhost:8080/api/bikes/${bikeId}/return-notes`)
      .subscribe({
        next: (notes) => {
          this.selectedBikeNotes.set(notes);
          this.showNotesModal.set(true);
        },
        error: (err) => this.showAlert('Could not load history', 'error')
      });
  }

  /* =========================================================
     DATA HELPERS
     ========================================================= */

  private tryLoadOffers() {
    this.extractUser();
    const token = localStorage.getItem('token');

    if (!this.bike.offeredBy || !token) {
      this.retryTimeout = setTimeout(() => this.tryLoadOffers(), 1000);
      return;
    }
    this.loadMyOffers();
  }

  private loadMyOffers() {
    if (!this.bike.offeredBy) return;

    this.isLoading = true;
    this.http.get<any[]>(`http://localhost:8080/api/bikes?offeredById=${this.bike.offeredBy}`)
      .pipe(finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (data) => this.myOffers = data || [],
        error: (err) => console.error('Error loading offers:', err)
      });
  }

  private extractUser() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.bike.offeredBy = payload.sub; // This is the user UUID
      } catch (e) {
        console.error('Error decoding token:', e);
      }
    }
  }

  showAlert(message: string, type: 'success' | 'error' | 'info' = 'info') {
    this.alert.set({ show: true, message, type });
    setTimeout(() => this.alert.set({ ...this.alert(), show: false }), 5000);
  }

  get isLoading(): boolean { return this._isLoading; }
  set isLoading(value: boolean) { this._isLoading = value; }
}