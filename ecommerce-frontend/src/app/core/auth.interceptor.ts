import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

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
        auth.logout();
        const returnUrl = router.url?.startsWith('/login') ? '/' : router.url || '/';
        void router.navigate(['/login'], { queryParams: { returnUrl } });
      }
      return throwError(() => err);
    })
  );
};
