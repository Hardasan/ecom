import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './core/auth.service';

/**
 * Landing route for the dashboard host (`/`). Sends each signed-in staff member to their own area
 * by role, and anyone else to the login page. Kept component-based (rather than a functional
 * redirect) so the decision reads the live auth state at navigation time.
 */
@Component({
  selector: 'app-dashboard-home',
  template: `<p style="padding:24px;text-align:center;color:#6b7280">در حال هدایت…</p>`
})
export class DashboardHome {
  constructor() {
    const auth = inject(AuthService);
    const router = inject(Router);
    const role = auth.role();
    const target = role === 'ROLE_ADMIN' ? '/admin' : role === 'ROLE_WAREHOUSE' ? '/warehouse' : '/login';
    void router.navigateByUrl(target);
  }
}
