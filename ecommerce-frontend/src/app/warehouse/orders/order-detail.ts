import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { WarehouseOrderService } from '../services/warehouse-order.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderStatusLabel, variantLabel } from '../../core/format';
import { orderStatusTone } from '../../admin/admin-format';

/** Common Iranian carriers offered as autocomplete suggestions (staff may type any other). */
const CARRIERS = ['پست پیشتاز', 'پست سفارشی', 'تیپاکس', 'چاپار', 'ماهکس', 'اسنپ‌باکس', 'الوپیک'];

@Component({
  selector: 'app-warehouse-order-detail',
  imports: [FormsModule, RouterLink],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.scss'
})
export class WarehouseOrderDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(WarehouseOrderService);

  readonly order = signal<OrderDto | null>(null);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly carriers = CARRIERS;

  carrier = '';
  trackingNumber = '';

  readonly date = formatFaDate;
  readonly money = formatPrice;
  readonly statusLabel = orderStatusLabel;
  readonly tone = orderStatusTone;
  readonly img = imageSrc;
  readonly variant = variantLabel;

  id = 0;

  readonly canApprove = computed(() => {
    const o = this.order();
    return !!o && (o.status === 'PAID' || (o.status === 'RESERVED' && o.paymentMethod === 'CASH_ON_DELIVERY'));
  });
  readonly canShip = computed(() => this.order()?.status === 'PROCESSING');
  readonly canDeliver = computed(() => this.order()?.status === 'SENDING');
  readonly canCancel = computed(() => {
    const s = this.order()?.status;
    return s === 'RESERVED' || s === 'PAID' || s === 'PROCESSING';
  });

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

  approve(): void {
    this.act(this.api.approve(this.id), 'سفارش تأیید شد و آماده‌سازی آغاز شد');
  }

  ship(): void {
    const carrier = this.carrier.trim();
    const tracking = this.trackingNumber.trim();
    if (!carrier || !tracking) {
      this.error.set('نام شرکت پستی و کد رهگیری را وارد کنید');
      return;
    }
    this.act(this.api.ship(this.id, { carrier, trackingNumber: tracking }), 'ارسال سفارش ثبت شد');
  }

  deliver(): void {
    if (confirm('تحویل این سفارش به مشتری ثبت شود؟')) {
      this.act(this.api.deliver(this.id), 'تحویل سفارش ثبت شد');
    }
  }

  cancel(): void {
    if (confirm('این سفارش لغو شود؟ موجودی کالاها به انبار بازمی‌گردد.')) {
      this.act(this.api.cancel(this.id), 'سفارش لغو شد');
    }
  }

  private act(obs: Observable<OrderDto>, msg: string): void {
    this.busy.set(true);
    this.error.set('');
    obs.subscribe({
      next: (o) => {
        this.order.set(o);
        this.busy.set(false);
        this.carrier = '';
        this.trackingNumber = '';
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
    setTimeout(() => this.toast.set(''), 2500);
  }
}
