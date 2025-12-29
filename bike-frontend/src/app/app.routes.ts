import { Routes } from '@angular/router';
import { LandingComponent } from './general/landing-component/landing-component';
import { Loginpage } from './user/loginpage/loginpage';
import { Dashboard } from './user/dashboard/dashboard';
import { Register } from './user/register/register';
import { Myrentals } from './myrentals/myrentals';
import { OfferBikeComponent } from './offerbike/offerbike';
import { Mainlayout } from './mainlayout/mainlayout';
import { MarketplaceComponent } from './marketplace/marketplace';
import { roleGuard } from './auth/role-guard';

export const routes: Routes = [
  // 1. PUBLIC PAGES
  { path: '', component: LandingComponent },
  { path: 'login', component: Loginpage },
  { path: 'register', component: Register },

  // 2. PROTECTED APP PAGES
  {
    path: '', 
    component: Mainlayout, 
    children: [
      { 
        path: 'dashboard', 
        component: Dashboard,
        canActivate: [roleGuard],
        data: { roles: ['STUDENT', 'EMPLOYEE'] } 
      }, 
      { 
        path: 'rentals', 
        component: Myrentals,
        canActivate: [roleGuard],
        data: { roles: ['STUDENT', 'EMPLOYEE'] } 
      },
      { 
        path: 'sales', 
        component: MarketplaceComponent,
        canActivate: [roleGuard],
        data: { roles: ['STUDENT', 'EMPLOYEE', 'ORDINARY'] } 
      },
      { 
        path: 'offer', 
        component: OfferBikeComponent,
        canActivate: [roleGuard],
        data: { roles: ['STUDENT', 'EMPLOYEE', 'EIFFEL_BIKE_CORP'] } 
      }
    ]
  },

  // 3. FALLBACK
  { path: '**', redirectTo: '' }
];