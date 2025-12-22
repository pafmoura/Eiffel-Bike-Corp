import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-marketplace',
  imports: [CommonModule],
  templateUrl: './marketplace.html',
  styleUrl: './marketplace.scss',
})
export class MarketplaceComponent implements OnInit {
  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';
  
  offers = signal<any[]>([]);
  basket = signal<any[]>([]);
  isOpen = false;

  count = computed(() => this.basket().length);
  total = computed(() => this.basket().reduce((acc, i) => acc + i.saleOffer.askingPriceEur, 0));

  ngOnInit() { this.loadOffers(); this.loadBasket(); }

  toggleBasket() { this.isOpen = !this.isOpen; }
  
  getHeader() { 
    const t = localStorage.getItem('token'); 
    return t ? new HttpHeaders().set('Authorization', `Bearer ${t}`) : new HttpHeaders(); 
  }

  loadOffers(q = '') { this.http.get<any[]>(`${this.baseUrl}/sales/offers?q=${q}`).subscribe(d => this.offers.set(d)); }
  search(e: any) { this.loadOffers(e.target.value); }

  loadBasket() { this.http.get<any>(`${this.baseUrl}/basket`, { headers: this.getHeader() }).subscribe(d => this.basket.set(d.items || [])); }

  addToBasket(id: number) { 
    this.http.post(`${this.baseUrl}/basket/items`, { saleOfferId: id }, { headers: this.getHeader() }).subscribe((d: any) => {
      this.basket.set(d.items);
      this.isOpen = true;
    }); 
  }

  remove(id: number) { 
    this.http.delete(`${this.baseUrl}/basket/items/${id}`, { headers: this.getHeader() }).subscribe((d: any) => this.basket.set(d.items)); 
  }

  checkout() { 
    this.http.post(`${this.baseUrl}/purchases/checkout`, {}, { headers: this.getHeader() }).subscribe(() => {
      alert('Purchase Successful!');
      this.basket.set([]);
      this.isOpen = false;
    });
  }
}