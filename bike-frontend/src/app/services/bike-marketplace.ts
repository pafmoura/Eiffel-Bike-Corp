// bike-marketplace.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BikeMarketplace {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';

  private getHeaders() {
    const token = localStorage.getItem('token');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Sales & Offers (US_12, US_13, US_14, US_15)
  getOffers(query: string = ''): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/sales/offers?q=${query}`);
  }

  getOfferDetails(offerId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/sales/offers/${offerId}`);
  }

  // Basket Management (US_16, US_17)
  getBasket(): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/basket`, { headers: this.getHeaders() });
  }

  addToBasket(saleOfferId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/basket/items`, { saleOfferId }, { headers: this.getHeaders() });
  }

removeFromBasket(saleOfferId: number): Observable<any> {
  const url = `${this.baseUrl}/basket/items/${saleOfferId}`; 
  
  return this.http.delete(url, { headers: this.getHeaders() });
}
  // Checkout & Payment (US_18, US_19)
  checkout(): Observable<any> {
    return this.http.post(`${this.baseUrl}/purchases/checkout`, {}, { headers: this.getHeaders() });
  }

  payPurchase(purchaseId: number, paymentData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/payments/purchases`, {
      purchaseId,
      ...paymentData // amount, currency, paymentMethodId
    }, { headers: this.getHeaders() });
  }
}