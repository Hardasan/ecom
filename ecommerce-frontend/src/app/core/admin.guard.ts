import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlSegment } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Gate for the `/admin` area. As a CanMatchFn the router won't even download the admin chunks for a
 * non-admin, keeping admin code out of the storefront bundle. Admins pass; guests go to login (with a
 * return url), and a signed-in non-admin is sent home.
 */
export const adminGuard: CanMatchFn = (_route, segments: UrlSegment[]) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.role() === 'ROLE_ADMIN') {
    return true;
  }
  const returnUrl = '/' + segments.map((s) => s.path).join('/');
  return auth.isLoggedIn()
    ? router.createUrlTree(['/'])
    : router.createUrlTree(['/login'], { queryParams: { returnUrl } });
};
