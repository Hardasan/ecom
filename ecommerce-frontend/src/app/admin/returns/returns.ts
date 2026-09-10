import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { AdminReturnService } from '../services/admin-return.service';
import { ReturnRequestDto, ReturnStatus } from '../../core/models';
import { formatFaDate, formatPrice } from '../../core/format';
import { returnReasonLabel } from '../../core/return.service';
import { returnStatusLabel, returnStatusTone } from '../admin-format';

const FILTERS: { key: ReturnStatus | 'ALL'; label: string }[] = [
  { key: 'REQUESTED', label: 'در انتظار بررسی' },
  { key: 'APPROVED', label: 'در انتظار انبار' },
  { key: 'RECEIVED', label: 'آماده بازپرداخت' },
  { key: 'REFUNDED', label: 'بازپرداخت‌شده' },
  { key: 'REJECTED', label: 'ردشده' },
  { key: 'ALL', label: 'همه' }
];

/**
 * Admin returns (مرجوعی) queue. A shopper's return lands here as REQUESTED; the admin approves or
 * rejects it, then — after paying the money back through the bank — records the transfer reference
 * to move it to REFUNDED (which restocks the lines and posts a REFUND transaction on the order).
 */
@Component({
  selector: 'app-admin-returns',
  imports: [FormsModule, RouterLink],
  templateUrl: './returns.html',
  styleUrl: './returns.scss'
})
export class ReturnsAdmin implements OnInit {
  private readonly api = inject(AdminReturnService);

  readonly returns = signal<ReturnRequestDto[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly filter = signal<ReturnStatus | 'ALL'>('REQUESTED');
  readonly filters = FILTERS;

  // Refund modal state.
  readonly refundTarget = signal<ReturnRequestDto | null>(null);
  refReference = '';
  refIban = 'IR';

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

  approve(r: ReturnRequestDto): void {
    this.act(r.id, this.api.approve(r.id), 'مرجوعی تأیید شد');
  }

  reject(r: ReturnRequestDto): void {
    if (confirm('این درخواست مرجوعی رد شود؟')) {
      this.act(r.id, this.api.reject(r.id), 'مرجوعی رد شد');
    }
  }

  openRefund(r: ReturnRequestDto): void {
    this.refReference = '';
    this.refIban = r.iban || 'IR';
    this.error.set('');
    this.refundTarget.set(r);
  }

  submitRefund(): void {
    const target = this.refundTarget();
    if (!target) {
      return;
    }
    if (!this.refReference.trim()) {
      this.error.set('کد پیگیری تراکنش را وارد کنید');
      return;
    }
    if (this.refIban && this.refIban !== 'IR' && !/^IR\d{24}$/.test(this.refIban)) {
      this.error.set('شبا باید IR و ۲۴ رقم باشد');
      return;
    }
    const body: { reference: string; iban?: string } = { reference: this.refReference.trim() };
    if (/^IR\d{24}$/.test(this.refIban)) {
      body.iban = this.refIban;
    }
    this.refundTarget.set(null);
    this.act(target.id, this.api.refund(target.id, body), 'بازپرداخت ثبت شد');
  }

  private act(id: number, obs: Observable<ReturnRequestDto>, msg: string): void {
    this.busy.set(true);
    this.error.set('');
    obs.subscribe({
      next: (updated) => {
        // Replace the row in place so the badge/actions update without a full reload.
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
