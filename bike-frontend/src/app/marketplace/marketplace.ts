import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BikeMarketplace } from '../services/bike-marketplace';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './marketplace.html'
})
export class MarketplaceComponent implements OnInit {
  private marketService = inject(BikeMarketplace);

  offers = signal<any[]>([]);
  basketItems = signal<any[]>([]);
  selectedOffer = signal<any | null>(null);

  isOpen = signal(false);
  isPaymentStep = signal(false);
  currentPurchaseId = signal<number | null>(null);

  // Payment form signals
  cardNumber = signal('');
  expiry = signal('');
  cvc = signal('');

  total = computed(() =>
    this.basketItems().reduce((acc, i) => acc + (i.unitPriceEurSnapshot || 0), 0)
  );

  ngOnInit() {
    this.loadOffers();
    this.loadBasket();
  }

  loadOffers(q = '') {
    this.marketService.getOffers(q).subscribe({
      next: data => this.offers.set(data),
      error: err => console.error(err)
    });
  }

  loadBasket() {
    this.marketService.getBasket().subscribe({
      next: data => this.basketItems.set(data.items || []),
      error: err => console.error(err)
    });
  }

  viewDetails(id: number) {
    this.marketService.getOfferDetails(id).subscribe({
      next: details => this.selectedOffer.set(details)
    });
  }

  addToBasket(id: number) {
    this.marketService.addToBasket(id).subscribe({
      next: res => {
        this.basketItems.set(res.items);
        this.isOpen.set(true);
      }
    });
  }

  remove(id?: number) {
    if (!id) return;
    this.marketService.removeFromBasket(id).subscribe({
      next: res => this.basketItems.set(res.items)
    });
  }

  startCheckout() {
    this.marketService.checkout().subscribe({
      next: purchase => {
        this.currentPurchaseId.set(purchase.id);
        this.isPaymentStep.set(true);
        this.isOpen.set(true);
      }
    });
  }

  confirmPayment() {
    if (!this.isValidCard()) {
      return alert('Please enter valid card details.');
    }

    const id = this.currentPurchaseId();
    if (!id) return;

    this.marketService.payPurchase(id, {
      purchaseId: id,
      amount: this.total(),
      currency: 'EUR',
      paymentMethodId: 'pm_card_visa'
    }).subscribe({
      next: () => {
        alert('Payment successful!');
        this.resetUI();
      }
    });
  }

  private resetUI() {
    this.basketItems.set([]);
    this.isPaymentStep.set(false);
    this.isOpen.set(false);
    this.currentPurchaseId.set(null);
    this.cardNumber.set('');
    this.expiry.set('');
    this.cvc.set('');
    this.loadOffers();
  }

  isValidCard() {
    return this.cardNumber().length >= 16 &&
           this.expiry().length >= 4 &&
           this.cvc().length >= 3;
  }

  closeDrawer() {
    this.isOpen.set(false);
    this.isPaymentStep.set(false);
  }
}
