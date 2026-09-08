import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { ReturnService } from '../../core/return.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderItemCount } from '../../core/format';

type OrderTab = 'current' | 'completed' | 'cancelled' | 'returned';

// Which order statuses fall under each tab (the «مرجوع شده» tab is keyed off return requests, below).
const CURRENT = ['RESERVED', 'PAID', 'PROCESSING', 'SENDING'];
const COMPLETED = ['RECEIVED'];
const CANCELLED = ['CANCEL_BY_USER', 'CANCEL_BY_ADMIN', 'FAILED'];

@Component({
  selector: 'app-orders',
  imports: [RouterLink, FaNumPipe],
  templateUrl: './orders.html',
  styleUrl: './orders.scss'
})
export class Orders implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly ordersApi = inject(OrderService);
  private readonly returnsApi = inject(ReturnService);
  private readonly router = inject(Router);

  readonly orders = signal<OrderDto[]>([]);
  // Order ids that have a return request — powers the «مرجوع شده» tab (the order status itself has no
  // "returned" state; a return lives in its own request record keyed by orderId).
  readonly returnedIds = signal<Set<number>>(new Set());
  readonly error = signal('');
  readonly loading = signal(true);
  readonly activeTab = signal<OrderTab>('current');

  readonly tabs: { key: OrderTab; label: string }[] = [
    { key: 'current', label: 'جاری' },
    { key: 'completed', label: 'تکمیل شده' },
    { key: 'cancelled', label: 'لغو شده' },
    { key: 'returned', label: 'مرجوع شده' }
  ];

  readonly filtered = computed(() =>
    this.orders().filter((o) => this.inTab(o, this.activeTab()))
  );

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/orders' } });
      return;
    }
    this.ordersApi.list().subscribe({
      next: (list) => {
        this.orders.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('سفارش‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
    // Best-effort: mark which orders have a return so the «مرجوع شده» tab can list them.
    this.returnsApi.list().subscribe({
      next: (rs) => this.returnedIds.set(new Set((rs ?? []).map((r) => r.orderId))),
      error: () => undefined
    });
  }

  private inTab(o: OrderDto, tab: OrderTab): boolean {
    switch (tab) {
      case 'current':
        return CURRENT.includes(o.status);
      case 'completed':
        return COMPLETED.includes(o.status);
      case 'cancelled':
        return CANCELLED.includes(o.status);
      case 'returned':
        return this.returnedIds().has(o.id);
    }
  }

  setTab(tab: OrderTab): void {
    this.activeTab.set(tab);
  }

  count(tab: OrderTab): number {
    return this.orders().filter((o) => this.inTab(o, tab)).length;
  }

  emptyLabel(): string {
    switch (this.activeTab()) {
      case 'current':
        return 'سفارش جاری ندارید';
      case 'completed':
        return 'سفارش تکمیل‌شده‌ای ندارید';
      case 'cancelled':
        return 'سفارش لغوشده‌ای ندارید';
      case 'returned':
        return 'سفارش مرجوع‌شده‌ای ندارید';
    }
  }

  dateLabel(order: OrderDto): string {
    return formatFaDate(order.createdAt);
  }

  tracking(order: OrderDto): string {
    return `کد پیگیری: ${order.id}`;
  }

  total(order: OrderDto): string {
    return formatPrice(order.totalCost);
  }

  qty(order: OrderDto): string {
    return `تعداد کل کالاها: ${orderItemCount(order.items)}`;
  }

  /** First three product thumbnails for the order card. */
  thumbs(order: OrderDto): string[] {
    return (order.items ?? [])
      .map((item) => imageSrc(item.mainImage))
      .filter((src) => !!src)
      .slice(0, 3);
  }

  /** How many product images beyond the three shown — rendered as a «+N» chip. */
  extraCount(order: OrderDto): number {
    const withImage = (order.items ?? []).filter((item) => !!imageSrc(item.mainImage)).length;
    return Math.max(0, withImage - 3);
  }
}
