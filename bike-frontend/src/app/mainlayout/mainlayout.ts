import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../services/user-service';


/**
 * Main layout component for the application.
 * Handles user information display and navigation.
 */
@Component({
  selector: 'app-mainlayout',
imports: [CommonModule, RouterModule],
  templateUrl: './mainlayout.html',
  styleUrl: './mainlayout.scss',
})
export class Mainlayout {
  userService = inject(UserService);

  // Reactive signal for username display
  
  username = computed(() => this.userService.currentUser()?.fullName || 'Guest');

  hasAccess(roles: string[]): boolean {
    return this.userService.hasRole(roles);
  }

  logout() {
    this.userService.logout();
  }
}