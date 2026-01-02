import { Component, inject } from '@angular/core';
import { UserService } from '../../services/user-service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

/**
 * Registration component for new users.
 * Handles user sign-up functionality.
 * 
 */
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
  fullName: '', 
  email: '',
  type: 'CUSTOMER',
  password: '' 
};

/**
 * Handles user registration process.
 */
  onRegister() {
  this.userService.register(this.user).subscribe({
    next: (res) => {
      console.log('Success!', res);
      this.router.navigate(['/login']);
    },
    error: (err) => {
      console.error('Full Error Object:', err);

      const errorMessage = err.error?.message || JSON.stringify(err.error);
      alert('Registration failed: ' + errorMessage);
    }
  });
}
}
