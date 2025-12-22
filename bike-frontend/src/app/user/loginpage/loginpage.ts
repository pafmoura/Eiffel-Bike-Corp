import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../services/user-service';
import { FormsModule } from '@angular/forms'; 

@Component({
  selector: 'app-loginpage',
  standalone: true,
  imports: [FormsModule], 
  templateUrl: './loginpage.html',
  styleUrl: './loginpage.scss',
})
export class Loginpage {
  private userService = inject(UserService);
  private router = inject(Router);

  credentials = { email: '', password: '' };

  onLogin() {
    this.userService.login(this.credentials).subscribe({
      next: (res: any) => { // Ensure res is typed or use any to access properties
        console.log('Login successful!', res);
        
        // --- FIX START ---
        // Store the token so other components/interceptors can use it
        localStorage.setItem('token', res.accessToken); 
        // --- FIX END ---

        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        alert('Login failed: Invalid email or password');
      }
    });
  }
}