import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Role-based route guard to restrict access based on user roles.
 * @param route Route information
 * @param state Router state information
 * @returns Whether the route can be activated
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  
  if (!token) {
    router.navigate(['/login']);
    return false;
  }

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const userRole = payload.type; 
    const allowedRoles = route.data['roles'] as Array<string>;

    if (allowedRoles.includes(userRole)) {
      return true;
    } else {
 
      router.navigate(['/']); 
      return false;
    }
  } catch (e) {
    router.navigate(['/login']);
    return false;
  }
};