import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { AdminReviewDto, Page, ReviewStatus } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminReviewService {
  private readonly http = inject(HttpClient);

  /** Cross-product moderation queue (GET /api/admin/reviews). */
  queue(
    opts: { status?: ReviewStatus | null; page?: number; size?: number } = {}
  ): Observable<Page<AdminReviewDto>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.status) {
      params = params.set('status', opts.status);
    }
    return this.http.get<Page<AdminReviewDto>>(`${API_BASE_URL}/admin/reviews`, { params });
  }

  moderate(productId: number, reviewId: number, status: ReviewStatus): Observable<AdminReviewDto> {
    return this.http.patch<AdminReviewDto>(
      `${API_BASE_URL}/products/${productId}/reviews/${reviewId}/status`,
      { status }
    );
  }

  delete(productId: number, reviewId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/products/${productId}/reviews/${reviewId}`);
  }
}
