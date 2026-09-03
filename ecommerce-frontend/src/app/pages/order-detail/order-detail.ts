import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { OrderService } from '../../core/order.service';
import { OrderDto, OrderItemDto } from '../../core/models';
import {
  colorHex,
  formatFaDate,
  formatPrice,
  imageSrc,
  orderStatusLabel,
  toNumber,
  variantLabel
} from '../../core/format';

@Component({
  selector: 'app-order-detail',
  imports: [RouterLink, FaNumPipe],
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
  readonly busy = signal(false);
  readonly toast = signal('');

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
    return orderStatusLabel(this.order()?.status, this.order()?.paymentMethod);
  }

  /** The buyer may cancel while the order is not yet shipped (RESERVED, PAID or being prepared). */
  canCancel(): boolean {
    const s = this.order()?.status;
    return s === 'RESERVED' || s === 'PAID' || s === 'PROCESSING';
  }

  /** The buyer confirms receipt once the order is on its way (SENDING). */
  canReceive(): boolean {
    return this.order()?.status === 'SENDING';
  }

  cancelOrder() {
    const o = this.order();
    if (!o || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.ordersApi.cancel(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.busy.set(false);
        this.flash('سفارش لغو شد');
      },
      error: (err) => {
        this.busy.set(false);
        this.flash(err?.error?.message ?? 'لغو سفارش ناموفق بود');
      }
    });
  }

  receiveOrder() {
    const o = this.order();
    if (!o || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.ordersApi.receive(o.id).subscribe({
      next: (updated) => {
        this.order.set(updated);
        this.busy.set(false);
        this.flash('دریافت سفارش ثبت شد');
      },
      error: (err) => {
        this.busy.set(false);
        this.flash(err?.error?.message ?? 'ثبت دریافت ناموفق بود');
      }
    });
  }

  private flash(message: string) {
    this.toast.set(message);
    setTimeout(() => this.toast.set(''), 2500);
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

  itemPrice(item: OrderItemDto): string {
    return formatPrice(item.lineTotal);
  }

  itemThumb(item: OrderItemDto): string {
    return imageSrc(item.mainImage);
  }
}
