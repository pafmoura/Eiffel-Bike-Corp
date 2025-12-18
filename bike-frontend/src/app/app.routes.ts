import { Routes } from '@angular/router';
import { LandingComponent } from './general/landing-component/landing-component';
import { Loginpage } from './user/loginpage/loginpage';
import { Dashboard } from './user/dashboard/dashboard';
import { Register } from './user/register/register';


export const routes: Routes = [
  { path: '', component: LandingComponent },         // http://localhost:4200/
  { path: 'login', component: Loginpage },     // http://localhost:4200/login
  { path: 'dashboard', component: Dashboard }, // http://localhost:4200/dashboard
  {path: 'register', component: Register}
];