import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminDiscountService } from '../services/admin-discount.service';
import { DiscountDto } from '../../core/models';
import { formatFaDate, formatPrice } from '../../core/format';
import { discountScopeLabel, discountTypeLabel } from '../admin-format';

@Component({
  selector: 'app-admin-discount-list',
  imports: [RouterLink],
  templateUrl: './discount-list.html',
  styleUrl: './discount-list.scss'
})
export class DiscountListAdmin implements OnInit {
  private readonly api = inject(AdminDiscountService);

  readonly discounts = signal<DiscountDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');

  readonly typeLabel = discountTypeLabel;
  readonly scopeLabel = discountScopeLabel;
  readonly date = formatFaDate;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.list().subscribe({
      next: (r) => {
        this.discounts.set(r.discounts ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('کدهای تخفیف خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  valueLabel(d: DiscountDto): string {
    return d.type === 'PERCENTAGE' ? `${d.value}٪` : formatPrice(d.value);
  }

  remove(d: DiscountDto): void {
    if (!confirm(`حذف کد «${d.code}»؟`)) {
      return;
    }
    this.api.delete(d.id!).subscribe({
      next: () => {
        this.flash('حذف شد');
        this.load();
      },
      error: (e) => this.error.set(e?.error?.message ?? 'حذف ناموفق بود')
    });
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
