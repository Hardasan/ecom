import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AdminOrderService } from '../services/admin-order.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderStatusLabel } from '../../core/format';
import { orderStatusTone } from '../admin-format';

@Component({
  selector: 'app-admin-order-detail',
  imports: [FormsModule, RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss'
})
export class OrderDetailAdmin implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AdminOrderService);

  readonly order = signal<OrderDto | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly showRefund = signal(false);

  refReference = '';
  refIban = 'IR';

  readonly date = formatFaDate;
  readonly money = formatPrice;
  readonly statusLabel = orderStatusLabel;
  readonly tone = orderStatusTone;
  readonly img = imageSrc;

  id = 0;

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.get(this.id).subscribe({
      next: (o) => {
        this.order.set(o);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('سفارش یافت نشد');
        this.loading.set(false);
      }
    });
  }

  canSend(): boolean {
    const s = this.order()?.status;
    return s === 'PAID' || s === 'PROCESSING';
  }

  canCancel(): boolean {
    const s = this.order()?.status;
    return s === 'RESERVED' || s === 'PAID' || s === 'PROCESSING';
  }

  canRefund(): boolean {
    const o = this.order();
    if (!o) {
      return false;
    }
    const cancelled = o.status === 'CANCEL_BY_USER' || o.status === 'CANCEL_BY_ADMIN';
    const paid = (o.transactions ?? []).some((t) => t.type === 'PAYMENT');
    const refunded = (o.transactions ?? []).some((t) => t.type === 'REFUND');
    return cancelled && paid && !refunded;
  }

  send(): void {
    this.act(this.api.send(this.id), 'سفارش به‌عنوان ارسال‌شده ثبت شد');
  }

  cancel(): void {
    if (confirm('این سفارش لغو شود؟')) {
      this.act(this.api.cancel(this.id), 'سفارش لغو شد');
    }
  }

  submitRefund(): void {
    if (!this.refReference.trim() || !/^IR\d{24}$/.test(this.refIban)) {
      this.error.set('کد پیگیری و شبای معتبر (IR + ۲۴ رقم) لازم است');
      return;
    }
    this.showRefund.set(false);
    this.act(
      this.api.refund(this.id, { reference: this.refReference.trim(), iban: this.refIban }),
      'بازپرداخت ثبت شد'
    );
  }

  private act(obs: Observable<OrderDto>, msg: string): void {
    this.busy.set(true);
    this.error.set('');
    obs.subscribe({
      next: (o) => {
        this.order.set(o);
        this.busy.set(false);
        this.flash(msg);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'عملیات ناموفق بود');
      }
    });
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
