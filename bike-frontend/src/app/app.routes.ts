import { Routes } from '@angular/router';
import { LandingComponent } from './general/landing-component/landing-component';
import { Loginpage } from './user/loginpage/loginpage';
import { Dashboard } from './user/dashboard/dashboard';
import { Register } from './user/register/register';
import { Myrentals } from './myrentals/myrentals';
import { OfferBikeComponent } from './offerbike/offerbike';
import { Mainlayout } from './mainlayout/mainlayout';
import { MarketplaceComponent } from './marketplace/marketplace';


export const routes: Routes = [
  // --------------------------------------------------------
  // 1. PUBLIC PAGES (No Sidebar, No Header)
  // --------------------------------------------------------
  { 
    path: '', 
    component: LandingComponent, 
    pathMatch: 'full' // Important: Only match exact empty path
  },
  { path: 'login', component: Loginpage },
  { path: 'register', component: Register },

  // --------------------------------------------------------
  // 2. APP PAGES (Wrapped inside MainLayout with Sidebar)
  // --------------------------------------------------------
  {
    path: '', 
    component: Mainlayout, // This component holds the Sidebar
    children: [
      // When user goes to /dashboard, load Dashboard inside MainLayout
      { path: 'dashboard', component: Dashboard }, 
      { path: 'rentals', component: Myrentals },
      { path: 'sales', component: MarketplaceComponent },
      { path: 'offer', component: OfferBikeComponent }
    ]
  },

  // --------------------------------------------------------
  // 3. FALLBACK (Redirect unknown routes to Landing or Login)
  // --------------------------------------------------------
  { path: '**', redirectTo: '' }
];