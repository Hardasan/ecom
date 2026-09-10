import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { WarehouseReturnService } from '../services/warehouse-return.service';
import { ReturnRequestDto, ReturnStatus } from '../../core/models';
import { formatFaDate, formatPrice } from '../../core/format';
import { returnReasonLabel } from '../../core/return.service';
import { returnStatusLabel, returnStatusTone } from '../../admin/admin-format';

const FILTERS: { key: ReturnStatus | 'ALL'; label: string }[] = [
  { key: 'APPROVED', label: 'در انتظار تحویل کالا' },
  { key: 'RECEIVED', label: 'تحویل‌شده' },
  { key: 'ALL', label: 'همه' }
];

/**
 * Warehouse returns console. Approved returns arrive here for physical inspection: the operator
 * accepts the goods (which restocks them and moves the return to RECEIVED, ready for the admin's
 * refund) or rejects them. Read-only for every other status.
 */
@Component({
  selector: 'app-warehouse-returns',
  imports: [RouterLink],
  templateUrl: './returns.html',
  styleUrl: './returns.scss'
})
export class WarehouseReturns implements OnInit {
  private readonly api = inject(WarehouseReturnService);

  readonly returns = signal<ReturnRequestDto[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly filter = signal<ReturnStatus | 'ALL'>('APPROVED');
  readonly filters = FILTERS;

  readonly date = formatFaDate;
  readonly money = formatPrice;
  readonly statusLabel = returnStatusLabel;
  readonly tone = returnStatusTone;
  readonly reasonLabel = returnReasonLabel;

  ngOnInit(): void {
    this.load();
  }

  select(key: ReturnStatus | 'ALL'): void {
    this.filter.set(key);
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    const status = this.filter() === 'ALL' ? null : (this.filter() as ReturnStatus);
    this.api.list(status).subscribe({
      next: (list) => {
        this.returns.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('لیست مرجوعی‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  accept(r: ReturnRequestDto): void {
    if (confirm('کالای برگشتی سالم است و به موجودی انبار افزوده شود؟')) {
      this.act(r.id, this.api.accept(r.id), 'کالا تحویل انبار شد');
    }
  }

  reject(r: ReturnRequestDto): void {
    if (confirm('این مرجوعی رد شود؟ کالا به موجودی افزوده نمی‌شود.')) {
      this.act(r.id, this.api.reject(r.id), 'مرجوعی رد شد');
    }
  }

  private act(id: number, obs: Observable<ReturnRequestDto>, msg: string): void {
    this.busy.set(true);
    this.error.set('');
    obs.subscribe({
      next: (updated) => {
        this.returns.update((list) => list.map((r) => (r.id === id ? updated : r)));
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
