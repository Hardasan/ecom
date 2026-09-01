import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Gate for the admin shell. As a CanMatchFn the router won't even download the admin chunks for a
 * non-admin. Anyone who is not signed in as an admin is sent to the admin login page; the login page
 * routes them back to `/admin` on success.
 */
export const adminGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.role() === 'ROLE_ADMIN' ? true : router.createUrlTree(['/login']);
};
