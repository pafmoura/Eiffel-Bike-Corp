import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../services/user-service';


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
  userService = inject(UserService);

  username = computed(() => this.userService.currentUser()?.fullName || 'Guest');

  hasAccess(roles: string[]): boolean {
    return this.userService.hasRole(roles);
  }

  logout() {
    this.userService.logout();
  }
}