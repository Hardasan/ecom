import { Component, OnInit, inject, signal } from '@angular/core';
import { AdminReviewService } from '../services/admin-review.service';
import { AdminReviewDto, ReviewStatus } from '../../core/models';
import { formatFaDate } from '../../core/format';
import { reviewStatusLabel, reviewStatusTone } from '../admin-format';

const FILTERS: { key: ReviewStatus | 'ALL'; label: string }[] = [
  { key: 'PENDING', label: 'در انتظار تأیید' },
  { key: 'PUBLISHED', label: 'منتشر شده' },
  { key: 'HIDDEN', label: 'پنهان' },
  { key: 'ALL', label: 'همه' }
];

@Component({
  selector: 'app-admin-reviews',
  imports: [],
  templateUrl: './reviews.html',
  styleUrl: './reviews.scss'
})
export class ReviewsAdmin implements OnInit {
  private readonly api = inject(AdminReviewService);

  readonly reviews = signal<AdminReviewDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');
  readonly filter = signal<ReviewStatus | 'ALL'>('PENDING');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly filters = FILTERS;

  readonly date = formatFaDate;
  readonly statusLabel = reviewStatusLabel;
  readonly tone = reviewStatusTone;

  ngOnInit(): void {
    this.load(0);
  }

  select(key: ReviewStatus | 'ALL'): void {
    this.filter.set(key);
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.error.set('');
    const status = this.filter() === 'ALL' ? null : (this.filter() as ReviewStatus);
    this.api.queue({ status, page, size: 20 }).subscribe({
      next: (res) => {
        this.reviews.set(res.content ?? []);
        this.page.set(res.number ?? 0);
        this.totalPages.set(res.totalPages ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('نظرات خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  moderate(r: AdminReviewDto, status: ReviewStatus): void {
    this.api.moderate(r.productId, r.id, status).subscribe({
      next: () => {
        this.flash('وضعیت به‌روزرسانی شد');
        this.load(this.page());
      },
      error: (e) => this.error.set(e?.error?.message ?? 'عملیات ناموفق بود')
    });
  }

  remove(r: AdminReviewDto): void {
    if (!confirm('این نظر حذف شود؟')) {
      return;
    }
    this.api.delete(r.productId, r.id).subscribe({
      next: () => {
        this.flash('نظر حذف شد');
        this.load(this.page());
      },
      error: (e) => this.error.set(e?.error?.message ?? 'حذف ناموفق بود')
    });
  }

  stars(n: number): string {
    return '★'.repeat(n) + '☆'.repeat(Math.max(0, 5 - n));
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
