import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { OrderDto, OrderItemDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderStatusLabel, toNumber } from '../../core/format';

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss'
})
export class OrderDetail implements OnInit {
  readonly a = ASSETS;
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly ordersApi = inject(OrderService);
  private readonly router = inject(Router);

  readonly order = signal<OrderDto | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: `/orders/${id || ''}` } });
      return;
    }
    if (!id) {
      this.error.set('سفارش پیدا نشد');
      return;
    }
    this.ordersApi.get(id).subscribe({
      next: (o) => this.order.set(o),
      error: () => this.error.set('سفارش پیدا نشد')
    });
  }

  trackingCode(): string {
    const o = this.order();
    return o ? String(o.id) : '—';
  }

  orderDate(): string {
    return formatFaDate(this.order()?.createdAt);
  }

  status(): string {
    return orderStatusLabel(this.order()?.status);
  }

  recipientName(): string {
    const o = this.order();
    if (!o) {
      return '';
    }
    return `${o.recipientFirstName ?? ''} ${o.recipientLastName ?? ''}`.trim() || '—';
  }

  recipientMobile(): string {
    return this.order()?.recipientMobile || '—';
  }

  deliveryAddress(): string {
    const o = this.order();
    if (!o) {
      return '';
    }
    const parts = [o.city, o.addressLine];
    if (o.plaque) {
      parts.push(`پلاک ${o.plaque}`);
    }
    if (o.unit) {
      parts.push(`واحد ${o.unit}`);
    }
    return parts.filter(Boolean).join('، ') || '—';
  }

  total(): string {
    return formatPrice(this.order()?.totalCost, 'ریال');
  }

  savings(): string | null {
    const o = this.order();
    if (!o?.items?.length) {
      return null;
    }
    let save = 0;
    for (const item of o.items) {
      const unit = toNumber(item.unitPrice);
      const disc = item.discountPrice != null ? toNumber(item.discountPrice) : 0;
      if (disc > 0 && disc < unit) {
        save += (unit - disc) * item.quantity;
      }
    }
    return save > 0 ? formatPrice(save, 'ریال') : null;
  }

  itemTitle(item: OrderItemDto): string {
    if (item.variantValue) {
      return `${item.productName} | ${item.variantValue}`;
    }
    return item.productName;
  }

  itemPrice(item: OrderItemDto): string {
    return formatPrice(item.lineTotal, 'تومان');
  }

  itemThumb(item: OrderItemDto): string {
    return imageSrc(item.mainImage);
  }
}
