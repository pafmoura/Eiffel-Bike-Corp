import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../services/user-service';
import { FormsModule } from '@angular/forms'; // 1. Add this import

@Component({
  selector: 'app-loginpage',
  standalone: true, // Ensure this is present if using imports
  imports: [FormsModule], // 2. Add this to your imports array
  templateUrl: './loginpage.html',
  styleUrl: './loginpage.scss',
})
export class Loginpage {
  private userService = inject(UserService);
  private router = inject(Router);

  credentials = { email: '', password: '' };

  onLogin() {
    this.userService.login(this.credentials).subscribe({
      next: (res) => {
        console.log('Login successful!', res);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        alert('Login failed: Invalid email or password');
      }
    });
  }
}