import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { ReviewService } from '../../core/review.service';
import { MyReviewDto } from '../../core/models';
import { displayName, formatFaDate } from '../../core/format';

/** Profile «نظرات» — the shopper's own product reviews across all products (view-only list). */
@Component({
  selector: 'app-my-reviews',
  imports: [RouterLink],
  templateUrl: './my-reviews.html',
  styleUrl: './my-reviews.scss'
})
export class MyReviewsPage implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly reviewsApi = inject(ReviewService);
  private readonly router = inject(Router);

  readonly reviews = signal<MyReviewDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/my-reviews' } });
      return;
    }
    this.reviewsApi.myReviews().subscribe({
      next: (res) => {
        this.reviews.set(res.content ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('نظرات خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  productName(r: MyReviewDto): string {
    return displayName({ localName: r.productLocalName, name: r.productName });
  }

  date(r: MyReviewDto): string {
    return formatFaDate(r.createdAt);
  }

  /** Five booleans, true up to the star rating — for rendering filled/empty stars. */
  stars(r: MyReviewDto): boolean[] {
    return Array.from({ length: 5 }, (_, i) => i < (r.rating ?? 0));
  }

  statusLabel(r: MyReviewDto): string {
    switch (r.status) {
      case 'PUBLISHED':
        return 'منتشر شده';
      case 'HIDDEN':
        return 'رد شده';
      default:
        return 'در انتظار تأیید';
    }
  }

  statusClass(r: MyReviewDto): string {
    switch (r.status) {
      case 'PUBLISHED':
        return 'badge badge--green';
      case 'HIDDEN':
        return 'badge badge--red';
      default:
        return 'badge badge--amber';
    }
  }
}
