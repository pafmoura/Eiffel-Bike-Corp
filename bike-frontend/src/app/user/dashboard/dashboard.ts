import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {
private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';
  bikes = signal<any[]>([]);

  // Images
  readonly bikeImages: string[] = [
    'https://cdn.shopify.com/s/files/1/0290/9382/2538/files/emtb29fsweb_f504cf91-1a89-499d-aa9a-298835327cd3.jpg?width=800&crop=center',
    'https://c02.purpledshub.com/uploads/sites/39/2023/04/Giant-Propel-Advanced-Pro-0-AXS-01-edbd219.jpg?webp=1&w=1200',
    'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcQuqnSeZPqSaCHmM8Wj8fqD8bPkHKd1HODlCw&s'
  ];

  ngOnInit() { this.loadBikes(); }

  loadBikes() {
    const token = localStorage.getItem('token');
    const headers = token ? new HttpHeaders().set('Authorization', `Bearer ${token}`) : new HttpHeaders();

    this.http.get<any[]>(`${this.baseUrl}/bikes/all`, { headers }).subscribe({
      next: (data) => {
        this.bikes.set(data.map(bike => ({
             ...bike, 
             imageUrl: this.bikeImages[Math.floor(Math.random() * this.bikeImages.length)]
        })));
      },
      error: () => console.error('Failed to load bikes.')
    });
  }

  rentBike(bikeId: number) {
    const token = localStorage.getItem('token');
    if (!token) { alert('Please log in first'); return; };

    const payload = JSON.parse(atob(token.split('.')[1]));
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    this.http.post(`${this.baseUrl}/rentals`, { bikeId, customerId: payload.sub, days: 3 }, { headers })
      .subscribe({
        next: (res: any) => {
          alert(res.result === 'RENTED' ? 'Success! Bike Rented.' : 'Added to Waiting List');
          this.loadBikes(); 
        },
        error: () => alert('Error processing rental')
      });
  }
}