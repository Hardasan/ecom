import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { storefrontUrl } from '../../core/host';

@Component({
  selector: 'app-admin-login',
  imports: [FormsModule],
  templateUrl: './admin-login.html',
  styleUrl: './admin-login.scss'
})
export class AdminLogin {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly shopUrl = storefrontUrl();
  readonly busy = signal(false);
  readonly error = signal('');

  mobile = '';
  password = '';

  constructor() {
    // An already-signed-in staff member skips the login page, straight to their own area.
    this.routeByRole(this.auth.role());
  }

  submit(): void {
    const mobile = this.mobile.replace(/\D/g, '');
    if (!/^09\d{9}$/.test(mobile) || !this.password) {
      this.error.set('شماره موبایل و رمز عبور را وارد کنید');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.auth.login(mobile, this.password).subscribe({
      next: (res) => {
        this.busy.set(false);
        if (!this.routeByRole(res.role)) {
          // A valid shopper account must not hold a session on the dashboard.
          this.auth.logout();
          this.error.set('این حساب به پنل دسترسی ندارد');
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'ورود ناموفق بود');
      }
    });
  }

  /** Sends a staff role to its area. Returns false for a role with no dashboard access. */
  private routeByRole(role: string | null): boolean {
    if (role === 'ROLE_ADMIN') {
      void this.router.navigateByUrl('/admin');
      return true;
    }
    if (role === 'ROLE_WAREHOUSE') {
      void this.router.navigateByUrl('/warehouse');
      return true;
    }
    return false;
  }
}
