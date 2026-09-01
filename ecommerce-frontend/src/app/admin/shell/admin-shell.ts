import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { storefrontUrl } from '../../core/host';

type NavItem = { path: string; label: string; exact: boolean };

@Component({
  selector: 'app-admin-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin-shell.html',
  styleUrl: './admin-shell.scss'
})
export class AdminShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuOpen = signal(false);
  readonly shopUrl = storefrontUrl();

  readonly nav: NavItem[] = [
    { path: '/admin', label: 'داشبورد', exact: true },
    { path: '/admin/products', label: 'محصولات', exact: false },
    { path: '/admin/categories', label: 'دسته‌بندی‌ها', exact: false },
    { path: '/admin/discounts', label: 'کدهای تخفیف', exact: false },
    { path: '/admin/orders', label: 'سفارش‌ها', exact: false },
    { path: '/admin/reviews', label: 'نظرات', exact: false }
  ];

  toggleMenu(): void {
    this.menuOpen.update((v) => !v);
  }

  closeMenu(): void {
    this.menuOpen.set(false);
  }

  logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
