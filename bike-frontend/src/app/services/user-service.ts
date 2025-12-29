import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = 'http://localhost:8080/api/users';

  private currentUserSignal = signal<any>(null);
  
  currentUser = computed(() => this.currentUserSignal());
  userType = computed(() => this.currentUserSignal()?.type || '');

  constructor() {
    this.initializeAuth();
  }

    register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

 private initializeAuth() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        // Extract the payload from the token directly
        const payload = JSON.parse(atob(token.split('.')[1]));
        
        // Set the signal using the payload data
        this.currentUserSignal.set({
          fullName: payload.fullName,
          email: payload.email,
          type: payload.type // This ensures 'type' is ALWAYS present
        });
      } catch (e) {
        console.error("Failed to parse token", e);
        this.logout();
      }
    }
  }

  


  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((res: any) => {
        if (res.token) {
          localStorage.setItem('token', res.token);
          
          const payload = JSON.parse(atob(res.token.split('.')[1]));
          const userData = { ...res, type: payload.type }; 
          
          localStorage.setItem('user', JSON.stringify(userData));
          this.currentUserSignal.set(userData); 
        }
      })
    );
  }



  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  hasRole(roles: string[]): boolean {
    const userType = this.userType();
    if (!userType) return false;
    return roles.includes(userType.toUpperCase());
  }
}