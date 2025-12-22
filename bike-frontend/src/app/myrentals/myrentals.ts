import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-myrentals',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './myrentals.html',
  styleUrl: './myrentals.scss',
})
export class Myrentals {

  private http = inject(HttpClient);
  private baseUrl = 'http://localhost:8080/api';

  notifications = signal<any[]>([]);
  
  returnForm = {
    rentalId: null,
    condition: 'GOOD',
    comment: ''
  };

  ngOnInit() {
    this.loadNotifications();
  }

  getHeaders() {
    const token = localStorage.getItem('token');
    return token ? new HttpHeaders().set('Authorization', `Bearer ${token}`) : new HttpHeaders();
  }

  getUserId() {
    const token = localStorage.getItem('token');
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub; // Assuming UUID is in 'sub'
  }

  loadNotifications() {
    const userId = this.getUserId();
    if (!userId) return;

    // US_10: Check notifications
    this.http.get<any[]>(`${this.baseUrl}/rentals/notifications?customerId=${userId}`, { headers: this.getHeaders() })
      .subscribe({
        next: (data) => this.notifications.set(data),
        error: (e) => console.error('Error loading notifications', e)
      });
  }

  returnBike() {
    const userId = this.getUserId();
    if (!this.returnForm.rentalId || !userId) {
      alert('Missing Rental ID or User not logged in');
      return;
    }

    const body = {
      authorCustomerId: userId,
      comment: this.returnForm.comment,
      condition: this.returnForm.condition
    };

    // US_08: Return bike
    this.http.post(`${this.baseUrl}/rentals/${this.returnForm.rentalId}/return`, body, { headers: this.getHeaders() })
      .subscribe({
        next: (res) => {
          alert('Bike returned successfully! Payment pending.');
          this.returnForm = { rentalId: null, condition: 'GOOD', comment: '' };
        },
        error: (err) => alert('Failed to return bike. Check ID.')
      });
  }
}
