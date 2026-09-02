import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Gate for the warehouse console. As a CanMatchFn the router never downloads the warehouse chunks
 * for someone without access. Warehouse operators and admins may enter (admins can operate the queue
 * too, mirroring the backend `hasAnyRole('WAREHOUSE','ADMIN')`); everyone else is bounced to login.
 */
export const warehouseGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const role = auth.role();
  return role === 'ROLE_WAREHOUSE' || role === 'ROLE_ADMIN'
    ? true
    : router.createUrlTree(['/login']);
};
