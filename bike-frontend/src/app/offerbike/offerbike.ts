import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, OnInit, OnDestroy, ChangeDetectorRef, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { UserService } from '../services/user-service';

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

  private userService = inject(UserService); 
  // --- State Signals ---
  showSaleModal = signal(false);
  showNotesModal = signal(false);
  selectedBikeForSale = signal<any>(null);
  selectedBikeNotes = signal<any[]>([]);
  alert = signal<AlertState>({ show: false, message: '', type: 'info' });
existingSaleBikeIds = signal<number[]>([]);
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

  /**
   * Cleans up any pending timeouts on component destroy.
   */
  ngOnDestroy(): void {
    if (this.retryTimeout) {
      clearTimeout(this.retryTimeout);
    }
  }


  /**
   * opens the sale modal for the selected bike.
   * @param bike bike selected for sale
   */
  openSaleModal(bike: any) {
    this.selectedBikeForSale.set(bike);
    this.salePrice = bike.rentalDailyRateEur * 10;
    this.saleNote = '';
    this.showSaleModal.set(true);
  }

  /**
   * Submits a sale offer for the selected bike.
   * @returns 
   */
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

  /**
   * Adds an initial condition note to the newly created sale offer.
   * @param saleOfferId SAle offer to apply the note
   */
  private addInitialSaleNote(saleOfferId: number) {
  const notePayload = {
    saleOfferId: saleOfferId,
    title: 'Initial Condition Note',
    content: this.saleNote,
    createdBy: this.bike.offeredBy  
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

  /**
   * Finalizes the sale offer creation process with success feedback.
   * 
   */
private finalizeSaleSuccess() {
    this.isLoading = false;
    this.showSaleModal.set(false);
    this.showAlert('Bike is now listed for sale!', 'success');
    this.loadMyOffers(); 
  }

  /**
   * TODO: wrap on a service
   */
  submitOffer() {
    if (!this.bike.offeredBy) return;

    this.isLoading = true;
    const payload = {
      description: this.bike.description,
      rentalDailyRateEur: this.bike.rentalDailyRateEur,
      offeredById: this.bike.offeredBy,
      offeredByType: this.userService.userType(),
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

  /**
   * TODO:Wrap on a service
   * Allows viewing the history of a bike's return notes.
   * @param bikeId Bike selected for history
   */
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
next: (bikes) => {
          this.myOffers = bikes || [];
          this.fetchExistingSaleOffers(); // Load sale status after getting bikes
        },        error: (err) => console.error('Error loading offers:', err)
      });
  }

  private fetchExistingSaleOffers() {
    this.http.get<any[]>('http://localhost:8080/api/sale-offers')
      .pipe(finalize(() => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: (sales) => {
          // Map to an array of bike IDs that are currently listed for sale
          const ids = sales.map(s => s.bikeId);
          this.existingSaleBikeIds.set(ids);
        },
        error: (err) => console.error('Error loading sale status:', err)
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