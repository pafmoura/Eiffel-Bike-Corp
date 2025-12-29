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

  // loginpage.ts
onLogin() {
  this.userService.login(this.credentials).subscribe({
    next: (res: any) => {
      const token = res.token || res.accessToken; 
      
      if (!token) {
        console.error('No token found in response!', res);
        return;
      }
      localStorage.setItem('token', token);

      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const userType = payload.type;

        if (userType === 'ORDINARY') {
          this.router.navigate(['/sales']);
        } else if (userType === 'EIFFEL_BIKE_CORP') {
          this.router.navigate(['/offer']);
        } else {
          this.router.navigate(['/dashboard']);
        }
      } catch (e) {
        console.error('Error decoding token on login', e);
        this.router.navigate(['/dashboard']);
      }
    },
    error: (err) => {
      console.error(err);
      alert('Login failed: Invalid email or password');
    }
  });
}
}