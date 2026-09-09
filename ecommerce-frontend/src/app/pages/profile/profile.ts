import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { WishlistService } from '../../core/wishlist.service';
import { BottomNav } from '../../shared/bottom-nav/bottom-nav';

/**
 * Profile hub (bottom-nav tab): avatar + phone, two stat cards (ongoing orders / wishlist) and the
 * destination menu. Every destination is now its own page with a back button (سفارشات, نظرات,
 * آدرس‌ها, تنظیمات کاربری, علاقه‌مندی‌ها); only «یادآوری‌ها» has no page yet.
 */
@Component({
  selector: 'app-profile',
  imports: [RouterLink, FaNumPipe, BottomNav],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly ordersApi = inject(OrderService);
  private readonly wishlistApi = inject(WishlistService);
  private readonly router = inject(Router);

  readonly error = signal('');
  readonly toast = signal('');
  // The two stat cards: ongoing orders (RESERVED/PAID/PROCESSING/SENDING) and wishlist size.
  readonly currentOrdersCount = signal(0);
  readonly wishlistCount = signal(0);

  firstName = '';
  lastName = '';
  mobile = '';

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/profile' } });
      return;
    }
    this.auth.getProfile().subscribe({
      next: (p) => {
        this.firstName = p.firstName ?? '';
        this.lastName = p.lastName ?? '';
        this.mobile = p.mobile ?? '';
      },
      error: () => undefined
    });
    this.ordersApi.list().subscribe({
      next: (list) => {
        const ongoing = ['RESERVED', 'PAID', 'PROCESSING', 'SENDING'];
        this.currentOrdersCount.set((list ?? []).filter((o) => ongoing.includes(o.status)).length);
      },
      error: () => undefined
    });
    this.wishlistApi.list().subscribe({
      next: (w) => this.wishlistCount.set((w?.items ?? []).length),
      error: () => undefined
    });
  }

  /** Shopper display name for the hub header; falls back to the mobile number. */
  displayName(): string {
    const name = `${this.firstName} ${this.lastName}`.trim();
    return name || this.mobile || 'کاربر ریونی';
  }

  logout() {
    this.auth.logout();
    void this.router.navigateByUrl('/');
  }

  /** «یادآوری‌ها» has no dedicated page yet. */
  comingSoon() {
    this.toast.set('این بخش به‌زودی اضافه می‌شود');
    setTimeout(() => this.toast.set(''), 2000);
  }
}
