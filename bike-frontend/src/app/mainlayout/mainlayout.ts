import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-mainlayout',
imports: [CommonModule, RouterModule],
  templateUrl: './mainlayout.html',
  styleUrl: './mainlayout.scss',
})
export class Mainlayout {
username = signal<string>('Guest');

  constructor() {
    this.getUserFromToken();
  }

  getUserFromToken() {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username.set(payload.fullName || payload.sub);
      } catch (e) {
        console.error('Error parsing token', e);
      }
    }
  }

  logout() {
    localStorage.removeItem('token');
    window.location.reload();
  }
}
