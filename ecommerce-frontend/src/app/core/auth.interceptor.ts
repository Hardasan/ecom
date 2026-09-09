import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Routes that actually require a session. A 401 elsewhere (home, catalog, guest cart…) must NOT
// bounce the shopper to /login — a stale/expired token on app open should silently drop to guest,
// not hijack the landing to the login page. Protected pages redirect themselves via their guards.
const PROTECTED_PREFIXES = [
  '/profile',
  '/account',
  '/orders',
  '/checkout',
  '/returns',
  '/wishlist',
  '/my-reviews',
  '/addresses'
];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  // The whole UI is Persian, so ask the API for Persian error/message bundles (Accept-Language).
  const headers: Record<string, string> = { 'Accept-Language': 'fa' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const authedReq = req.clone({ setHeaders: headers });

  return next(authedReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && token) {
        // The token is no longer valid — drop the session either way.
        auth.logout();
        // …but only send them to /login if they're actually on a page that needs auth. On a public
        // page (home/catalog) we stay put and continue as a guest, so app-open never lands on login.
        const url = (router.url || '/').split('?')[0];
        const onProtected = PROTECTED_PREFIXES.some((p) => url === p || url.startsWith(p + '/'));
        if (onProtected) {
          const returnUrl = url.startsWith('/login') ? '/' : router.url || '/';
          void router.navigate(['/login'], { queryParams: { returnUrl } });
        }
      }
      return throwError(() => err);
    })
  );
};
