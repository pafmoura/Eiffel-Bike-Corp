// fx-rate.service.ts
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, of, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class FxRateService {
  private http = inject(HttpClient);

  // Initialize with EUR so the list isn't totally empty while loading
  private rates = signal<Record<string, number>>({ 'EUR': 1 });
  private selectedCurrency = signal('EUR');

  constructor() {
    this.loadRates();
  }
// fx-rate-service.ts
loadRates() {
  // Use the local proxy prefix defined in proxy.conf.json
  this.http.get<any>('/fx-api/v6/latest/EUR')
    .pipe(
      tap(res => {
        if (res && res.rates) {
          this.rates.set(res.rates);
        }
      }),
      catchError(err => {
        console.error('FX Load Failed:', err);
        return of(null);
      })
    ).subscribe();
}

  currency = computed(() => this.selectedCurrency());
  currencies = computed(() => Object.keys(this.rates()).sort());

  setCurrency(c: string) {
    this.selectedCurrency.set(c);
  }

  convert(eur: number) {
    const rate = this.rates()[this.currency()] || 1;
    return eur * rate;
  }
}