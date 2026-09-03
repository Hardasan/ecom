import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { OrderDto, OrderItemDto } from '../../core/models';
import { colorHex, formatPrice, toNumber, variantLabel } from '../../core/format';

@Component({
  selector: 'app-success',
  imports: [RouterLink, FaNumPipe],
  templateUrl: './success.html',
  styleUrl: './success.scss'
})
export class Success implements OnInit {
  readonly a = ASSETS;
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly ordersApi = inject(OrderService);

  readonly order = signal<OrderDto | null>(null);
  readonly error = signal('');

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('orderId'));
    if (!id || !this.auth.isLoggedIn()) {
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
    const raw = this.order()?.createdAt;
    if (!raw) {
      return '';
    }
    try {
      return new Intl.DateTimeFormat('fa-IR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      }).format(new Date(raw));
    } catch {
      return '';
    }
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
    return formatPrice(this.order()?.totalCost);
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
    return save > 0 ? formatPrice(save) : null;
  }

  /** CSS color for a COLOR variant line, or '' when the value is not a hex code. */
  variantHex(item: OrderItemDto): string {
    return colorHex(item.variantValue);
  }

  /** Readable variant label: color name (or hex) for COLOR, raw value otherwise. */
  variantText(item: OrderItemDto): string {
    return variantLabel(item.variantType, item.variantValue);
  }
}
