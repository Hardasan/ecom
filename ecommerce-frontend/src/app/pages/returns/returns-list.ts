import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { OrderDto } from '../../core/models';
import { ReturnService } from '../../core/return.service';
import { formatFaDate, formatPrice, orderItemCount, toFa } from '../../core/format';

/**
 * Returns — step 1 (screen «مرجوعی سفارش»): the shopper's orders still eligible to return
 * (RECEIVED, within the 7-day window). Tapping one starts the return flow for that order.
 */
@Component({
  selector: 'app-returns-list',
  imports: [RouterLink],
  templateUrl: './returns-list.html',
  styleUrl: './returns.scss'
})
export class ReturnsList implements OnInit {
  readonly a = ASSETS;
  private readonly returnsApi = inject(ReturnService);

  readonly orders = signal<OrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.returnsApi.returnableOrders().subscribe({
      next: (list) => {
        this.orders.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('دریافت سفارش‌ها ناموفق بود. لطفاً دوباره تلاش کنید.');
        this.loading.set(false);
      }
    });
  }

  code(o: OrderDto): string {
    return toFa(o.id);
  }

  total(o: OrderDto): string {
    return formatPrice(o.totalCost);
  }

  count(o: OrderDto): string {
    return toFa(orderItemCount(o.items));
  }

  date(o: OrderDto): string {
    return formatFaDate(o.deliveredAt || o.createdAt);
  }
}
