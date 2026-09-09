import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { MyReviewDto, PageResponse } from './models';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);

  /** The signed-in shopper's own reviews across all products (newest first). */
  myReviews(page = 0, size = 50): Observable<PageResponse<MyReviewDto>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<MyReviewDto>>(`${API_BASE_URL}/user/reviews`, { params });
  }
}
