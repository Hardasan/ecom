import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { WarehouseOrderService } from '../services/warehouse-order.service';
import { OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, orderItemCount, orderStatusLabel } from '../../core/format';
import { orderStatusTone } from '../../admin/admin-format';

type StageKey = 'NEW' | 'PROCESSING' | 'SENDING' | 'RECEIVED' | 'ALL';

const STAGES: { key: StageKey; label: string }[] = [
  { key: 'NEW', label: 'در انتظار تأیید' },
  { key: 'PROCESSING', label: 'در حال آماده‌سازی' },
  { key: 'SENDING', label: 'در حال ارسال' },
  { key: 'RECEIVED', label: 'تحویل شده' },
  { key: 'ALL', label: 'همه' }
];

@Component({
  selector: 'app-warehouse-order-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './order-list.html',
  styleUrl: './order-list.scss'
})
export class WarehouseOrderList implements OnInit {
  private readonly api = inject(WarehouseOrderService);

  readonly all = signal<OrderDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly filter = signal<StageKey>('NEW');
  readonly query = signal('');
  readonly stages = STAGES;

  readonly date = formatFaDate;
  readonly money = formatPrice;
  readonly statusLabel = orderStatusLabel;
  readonly tone = orderStatusTone;
  readonly count = orderItemCount;

  /** Per-stage counts for the tab badges. */
  readonly counts = computed(() => {
    const list = this.all();
    return {
      NEW: list.filter((o) => this.isNew(o)).length,
      PROCESSING: list.filter((o) => o.status === 'PROCESSING').length,
      SENDING: list.filter((o) => o.status === 'SENDING').length,
      RECEIVED: list.filter((o) => o.status === 'RECEIVED').length,
      ALL: list.length
    } as Record<StageKey, number>;
  });

  readonly visible = computed(() => {
    const key = this.filter();
    const q = this.query().trim();
    let list = this.all().filter((o) => this.inStage(o, key));
    if (q) {
      list = list.filter((o) =>
        `${o.id} ${o.recipientFirstName ?? ''} ${o.recipientLastName ?? ''} ${o.recipientMobile ?? ''} ${o.trackingNumber ?? ''}`.includes(
          q
        )
      );
    }
    return list;
  });

  ngOnInit(): void {
    this.api.list().subscribe({
      next: (l) => {
        this.all.set(l ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('سفارش‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  select(key: StageKey): void {
    this.filter.set(key);
  }

  stageCount(key: StageKey): number {
    return this.counts()[key];
  }

  paymentLabel(o: OrderDto): string {
    return o.paymentMethod === 'CASH_ON_DELIVERY' ? 'پرداخت در محل' : 'آنلاین';
  }

  /** Orders awaiting the operator's first action: paid online, or a COD order still reserved. */
  private isNew(o: OrderDto): boolean {
    return o.status === 'PAID' || (o.status === 'RESERVED' && o.paymentMethod === 'CASH_ON_DELIVERY');
  }

  private inStage(o: OrderDto, key: StageKey): boolean {
    switch (key) {
      case 'ALL':
        return true;
      case 'NEW':
        return this.isNew(o);
      default:
        return o.status === key;
    }
  }
}
