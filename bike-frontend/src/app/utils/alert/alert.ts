import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
export type AlertType = 'success' | 'error' | 'info';
@Component({
  selector: 'app-alert',
  imports: [CommonModule],
  templateUrl: './alert.html',
  styleUrl: './alert.scss',
})
/**
 * Alert utility to display messages to users.
 * Supports different alert types: success, error, and info.
 */
export class Alert {
message = signal<string | null>(null);
  type = signal<AlertType>('info');

  show(msg: string, type: AlertType = 'info') {
    this.message.set(msg);
    this.type.set(type);
    setTimeout(() => this.close(), 5000); 
  }

  close() {
    this.message.set(null);
  }
}
