import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BikeMarketplace } from '../services/bike-marketplace';
import { FxRateService } from '../fx-rate-service';
import { UserService } from '../services/user-service';

@Component({
  selector: 'app-marketplace',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyPipe],
  templateUrl: './marketplace.html'
})
export class MarketplaceComponent implements OnInit {
  private marketService = inject(BikeMarketplace);
  // Injecting the FX service as public so the template can access its signals
  public fx = inject(FxRateService);

    private userService = inject(UserService);

currentUserId = signal<string | null>(null);


  offers = signal<any[]>([]);
  basketItems = signal<any[]>([]);
  selectedOffer = signal<any | null>(null);
allBikes = signal<any[]>([]);

  isOpen = signal(false);
  isPaymentStep = signal(false);
  currentPurchaseId = signal<number | null>(null);

  // Payment form signals
  cardNumber = signal('');
  expiry = signal('');
  cvc = signal('');

  // Total always remains in EUR for backend consistency
  totalEur = computed(() =>
    this.basketItems().reduce((acc, i) => acc + (i.unitPriceEurSnapshot || 0), 0)
  );

  ngOnInit() {

        const user = this.userService.currentUser();
    if (user) {
      this.currentUserId.set(user.id);
    }

    this.loadOffers();
    this.loadBasket();
    this.loadAllBikes();

    

  }

  loadAllBikes() {
  this.marketService.getAllBikes().subscribe({
    next: bikes => this.allBikes.set(bikes),
    error: err => console.error(err)
  });
}

getBikeDescription(bikeId: number) {
  const bike = this.allBikes().find(b => b.id === bikeId);
  return bike ? bike.description : `Bike #${bikeId}`;
}



  loadOffers(q = '') {
    this.marketService.getOffers(q).subscribe({
      next: data => {this.offers.set(data)

            console.log(this.offers())

      },
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

  
isAddable(offer: any): boolean {
  const bike = this.allBikes().find(b => b.id === offer.bikeId);
  return bike ? bike.offeredBy.id !== this.currentUserId() : false;
}

addToBasket(offerId: number) {
  const offer = this.offers().find(o => o.id === offerId);
  if (!offer) return;

  const bike = this.allBikes().find(b => b.id === offer.bikeId);
  if (!bike) return;

  console.log("bike found:", bike);
  console.log("current user id:", this.currentUserId());

  if (bike.offeredBy.id === this.currentUserId()) {
    return alert("You cannot add your own bike to the basket.");
  }

  this.marketService.addToBasket(offerId).subscribe({
    next: res => {
      this.basketItems.set(res.items);
      this.isOpen.set(true);
    },
    error: err => console.error(err)
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

    // Payment sent in EUR as per original logic
    this.marketService.payPurchase(id, {
      purchaseId: id,
      amount: this.totalEur(),
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