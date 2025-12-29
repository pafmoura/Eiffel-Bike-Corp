import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';


/**
 * Main layout component that includes the navigation bar and router outlet.
 * Displays the username extracted from the JWT token stored in localStorage.
 * Provides a logout function to clear the token and reload the app.
 * Uses Angular signals for reactive state management.
 */
@Component({
  selector: 'app-mainlayout',
imports: [CommonModule, RouterModule],
  templateUrl: './mainlayout.html',
  styleUrl: './mainlayout.scss',
})
export class Mainlayout {
username = signal<string>('Guest');
userType = signal<string>(''); 
  constructor(private router: Router) {
    this.getUserFromToken();
  }

getUserFromToken() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username.set(payload.fullName || payload.sub);
        this.userType.set(payload.type); // Store the role
      } catch (e) {
        console.error('Error parsing token', e);
      }
    }
  }

  // Helper method for the template
  hasAccess(roles: string[]): boolean {
    return roles.includes(this.userType());
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}