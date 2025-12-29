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

  private initializeAuth() {
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    if (token && user) {
      try {
        this.currentUserSignal.set(JSON.parse(user));
      } catch (e) {
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
    return roles.includes(this.userType());
  }
}