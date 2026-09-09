import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';

/**
 * Account settings — a dedicated page (profile menu «تنظیمات کاربری») with a back button, replacing
 * the old inline account-edit section. Edits the shopper's name + mobile.
 */
@Component({
  selector: 'app-account',
  imports: [FormsModule, RouterLink],
  templateUrl: './account.html',
  styleUrl: './account.scss'
})
export class AccountPage implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');

  firstName = '';
  lastName = '';
  mobile = '';

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/account' } });
      return;
    }
    this.auth.getProfile().subscribe({
      next: (p) => {
        this.firstName = p.firstName ?? '';
        this.lastName = p.lastName ?? '';
        this.mobile = p.mobile ?? '';
      },
      error: () => this.error.set('اطلاعات حساب خوانده نشد')
    });
  }

  saveProfile() {
    const mobile = this.mobile.replace(/\D/g, '');
    if (!this.firstName.trim() || !this.lastName.trim() || !/^09\d{9}$/.test(mobile)) {
      this.error.set('نام، نام خانوادگی و شماره موبایل معتبر لازم است');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.auth
      .updateProfile({ firstName: this.firstName.trim(), lastName: this.lastName.trim(), mobile })
      .subscribe({
        next: (p) => {
          this.mobile = p.mobile;
          this.busy.set(false);
          this.flash('اطلاعات حساب ذخیره شد');
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err?.error?.message ?? 'ذخیره اطلاعات ناموفق بود');
        }
      });
  }

  private flash(message: string) {
    this.toast.set(message);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
