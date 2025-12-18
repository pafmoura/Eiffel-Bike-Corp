import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/users';

  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((res: any) => {
        // res should contain { token: '...', id: '...', name: '...' }
        if (res.token) {
          localStorage.setItem('token', res.token);
          // Optional: Store user info for the dashboard to display
          localStorage.setItem('user', JSON.stringify(res)); 
        }
      })
    );
  }

  // Helper to get the token for API calls
  getToken() {
    return localStorage.getItem('token');
  }

  // Helper to get user details for the Dashboard UI
  getUserData() {
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login'; // Redirect on logout
  }
}