import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html'
})
export class Dashboard implements OnInit {
  private http = inject(HttpClient);
  bikes = signal<any[]>([]);
  
  // Base API URL
  private baseUrl = 'http://localhost:8080/api';

  ngOnInit() {
    this.loadBikes();
  }

  loadBikes() {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);

    this.http.get<any[]>(`${this.baseUrl}/bikes/all`, { headers })
      .subscribe({
        next: (data) => this.bikes.set(data),
        error: () => alert('Session expired. Please login again.')
      });
  }

  rentBike(bikeId: number) {
    const token = localStorage.getItem('token');
    if (!token) return;

    // 1. Grab userId from token payload
    const payload = JSON.parse(atob(token.split('.')[1]));
    const userId = payload.sub; // Or payload.userId depending on your Java Backend

    const headers = new HttpHeaders().set('Authorization', `Bearer ${token}`);
    
    const body = {
      bikeId: bikeId,
      customerId: userId, // Effectively substituting the context here
      days: 3
    };

    this.http.post(`${this.baseUrl}/rentals`, body, { headers })
      .subscribe({
        next: (res: any) => {
          alert(res.result === 'RENTED' ? 'Success!' : 'Added to Waiting List');
        },
        error: () => alert('Error processing rental')
      });
  }
}