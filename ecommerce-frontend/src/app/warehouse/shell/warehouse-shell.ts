import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { storefrontUrl } from '../../core/host';

type NavItem = { path: string; label: string; exact: boolean };

/**
 * Shell for the warehouse operator console on the dashboard host. Mirrors the admin shell layout,
 * but its own nav (the fulfillment queue) and branding.
 */
@Component({
  selector: 'app-warehouse-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './warehouse-shell.html',
  styleUrl: './warehouse-shell.scss'
})
export class WarehouseShell {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly menuOpen = signal(false);
  readonly shopUrl = storefrontUrl();

  readonly nav: NavItem[] = [{ path: '/warehouse', label: 'صف سفارش‌ها', exact: true }];

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
