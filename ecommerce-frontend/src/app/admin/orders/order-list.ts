import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AdminOrderService } from '../services/admin-order.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, orderItemCount, orderStatusLabel } from '../../core/format';
import { orderStatusTone } from '../admin-format';

const FILTERS = [
  { key: 'ALL', label: 'همه' },
  { key: 'RESERVED', label: 'رزرو شده' },
  { key: 'PAID', label: 'پرداخت شده' },
  { key: 'PROCESSING', label: 'در حال آماده‌سازی' },
  { key: 'SENDING', label: 'در حال ارسال' },
  { key: 'RECEIVED', label: 'تحویل شده' },
  { key: 'CANCELLED', label: 'لغو/ناموفق' },
  { key: 'REFUNDABLE', label: 'قابل بازپرداخت' }
] as const;

@Component({
  selector: 'app-admin-order-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss'
})
export class OrderListAdmin implements OnInit {
  private readonly api = inject(AdminOrderService);

  readonly allOrders = signal<OrderDto[]>([]);
  readonly refundableOrders = signal<OrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly filter = signal<string>('ALL');
  readonly query = signal('');
  readonly filters = FILTERS;

  private refundableLoaded = false;

  readonly date = formatFaDate;
  readonly money = formatPrice;
  readonly statusLabel = orderStatusLabel;
  readonly tone = orderStatusTone;
  readonly count = orderItemCount;

  readonly visible = computed(() => {
    const key = this.filter();
    const q = this.query().trim();
    let list = key === 'REFUNDABLE' ? this.refundableOrders() : this.applyStatus(this.allOrders(), key);
    if (q) {
      list = list.filter((o) =>
        `${o.id} ${o.recipientFirstName ?? ''} ${o.recipientLastName ?? ''} ${o.recipientMobile ?? ''}`.includes(q)
      );
    }
    return list;
  });

  ngOnInit(): void {
    this.api.list().subscribe({
      next: (l) => {
        this.allOrders.set(l ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('سفارش‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  select(key: string): void {
    this.filter.set(key);
    if (key === 'REFUNDABLE' && !this.refundableLoaded) {
      this.api.refundable().subscribe({
        next: (l) => {
          this.refundableOrders.set(l ?? []);
          this.refundableLoaded = true;
        },
        error: () => this.error.set('لیست بازپرداخت خوانده نشد')
      });
    }
  }

  private applyStatus(list: OrderDto[], key: string): OrderDto[] {
    if (key === 'ALL' || key === 'REFUNDABLE') {
      return list;
    }
    if (key === 'CANCELLED') {
      return list.filter(
        (o) => o.status === 'CANCEL_BY_USER' || o.status === 'CANCEL_BY_ADMIN' || o.status === 'FAILED'
      );
    }
    return list.filter((o) => o.status === key);
  }
}
