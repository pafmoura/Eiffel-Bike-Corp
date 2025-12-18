import { Component, inject } from '@angular/core';
import { UserService } from '../../services/user-service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  imports: [FormsModule, CommonModule],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register {
private userService = inject(UserService);
  private router = inject(Router);

  user = {
  fullName: '', // Changed from 'name' to 'fullName'
  email: '',
  type: 'CUSTOMER',
  password: '' // ed mandatory field. Ensure 'CUSTOMER' matches your UserType Enum
};

  onRegister() {
  this.userService.register(this.user).subscribe({
    next: (res) => {
      console.log('Success!', res);
      this.router.navigate(['/login']);
    },
    error: (err) => {
      // 1. Log the full object to the browser console (Press F12 to see it)
      console.error('Full Error Object:', err);

      // 2. Try to show the specific message from the Java backend
      const errorMessage = err.error?.message || JSON.stringify(err.error);
      alert('Registration failed: ' + errorMessage);
    }
  });
}
}
