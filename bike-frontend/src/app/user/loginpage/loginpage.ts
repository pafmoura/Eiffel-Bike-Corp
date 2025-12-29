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
      next: () => {
        const userType = this.userService.userType();
        if (userType === 'ORDINARY') this.router.navigate(['/sales']);
        else if (userType === 'EIFFEL_BIKE_CORP') this.router.navigate(['/offer']);
        else this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error(err);
        alert('Login failed: Invalid email or password');
      }
    });
  }
}
