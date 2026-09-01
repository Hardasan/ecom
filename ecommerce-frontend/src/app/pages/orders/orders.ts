import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderItemCount, orderStatusLabel } from '../../core/format';

@Component({
  selector: 'app-orders',
  imports: [RouterLink],
  templateUrl: './orders.html',
  styleUrl: './orders.scss'
})
export class Orders implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly ordersApi = inject(OrderService);
  private readonly router = inject(Router);

  readonly orders = signal<OrderDto[]>([]);
  readonly error = signal('');
  readonly loading = signal(true);

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

  status(order: OrderDto): string {
    return orderStatusLabel(order.status, order.paymentMethod);
  }

  thumbs(order: OrderDto): string[] {
    return (order.items ?? [])
      .map((item) => imageSrc(item.mainImage))
      .filter((src) => !!src)
      .slice(0, 4);
  }
}
