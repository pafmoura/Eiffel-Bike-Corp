import { Injectable, inject, signal, computed, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private platformId = inject(PLATFORM_ID); // Detects Browser vs Server
  private apiUrl = 'http://localhost:8080/api/users';

  // --- Signals ---
  private currentUserSignal = signal<any>(null);
  currentUser = computed(() => this.currentUserSignal());
  userType = computed(() => this.currentUserSignal()?.type || '');

  constructor() {
    // Initialize user from token/localStorage if in browser
    if (isPlatformBrowser(this.platformId)) {
      const token = localStorage.getItem('token');
      const user = localStorage.getItem('user');

      if (token) {
        this.setUserFromToken(token);
      } else if (user) {
        this.currentUserSignal.set(JSON.parse(user));
      }
    }
  }

  // --- Registration ---
  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  // --- Login ---
  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((res: any) => {
        if (!isPlatformBrowser(this.platformId)) return;

        // Prefer backend token if present
        const token = res.token || res.accessToken;
        if (token) {
          localStorage.setItem('token', token);
        }

        // Store user info for fallback and dashboard
        if (res) {
          localStorage.setItem('user', JSON.stringify(res));
        }

        // Try decoding JWT, fallback to user object if fails
        this.setUserFromToken(token);
      })
    );
  }

  // --- Logout ---
  logout() {
    if (isPlatformBrowser(this.platformId)) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
    this.currentUserSignal.set(null);
    this.router.navigate(['/login']);
  }

  // --- Role Check ---
  hasRole(roles: string[]): boolean {
    const type = this.userType();
    return type ? roles.includes(type.toUpperCase()) : false;
  }

  // --- Helper: decode token or fallback ---
  private setUserFromToken(token?: string) {
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));

        if (!payload.sub || !payload.type) throw new Error('Incomplete JWT payload');

        this.currentUserSignal.set({
          id: payload.sub,
          fullName: payload.fullName,
          email: payload.email,
          type: payload.type
        });
        return;
      }
    } catch (error) {
      console.warn('JWT decode failed, falling back to localStorage', error);
    }

    // Fallback to stored user
    const user = localStorage.getItem('user');
    if (user) {
      this.currentUserSignal.set(JSON.parse(user));
    } else {
      this.currentUserSignal.set(null);
    }
  }

  // --- Helper to get raw token ---
  getToken(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem('token');
  }

  // --- Helper to get stored user ---
  getUserData(): any | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    const user = localStorage.getItem('user');
    return user ? JSON.parse(user) : null;
  }
}
