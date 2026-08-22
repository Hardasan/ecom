import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { Page, ProductDto, ReviewDto, ReviewSummaryDto } from './models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private specialSaleCache: ProductDto[] | null = null;

  constructor(private readonly http: HttpClient) {}

  search(opts: {
    page?: number;
    size?: number;
    categoryId?: number;
    subCategoryId?: number;
    isAvailable?: boolean;
    localName?: string;
  } = {}): Observable<Page<ProductDto>> {
    let params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 20));
    if (opts.categoryId != null) {
      params = params.set('categoryId', String(opts.categoryId));
    }
    if (opts.subCategoryId != null) {
      params = params.set('subCategoryId', String(opts.subCategoryId));
    }
    if (opts.isAvailable != null) {
      params = params.set('isAvailable', String(opts.isAvailable));
    }
    if (opts.localName) {
      params = params.set('localName', opts.localName);
    }
    return this.http.get<Page<ProductDto>>(`${API_BASE_URL}/products`, { params });
  }

  getById(id: number): Observable<ProductDto> {
    return this.http.get<ProductDto>(`${API_BASE_URL}/products/${id}`);
  }

  specialSale(): Observable<{ products: ProductDto[] }> {
    if (this.specialSaleCache) {
      return of({ products: this.specialSaleCache });
    }
    return this.http.get<{ products: ProductDto[] }>(`${API_BASE_URL}/products/special-sale`).pipe(
      tap((res) => {
        this.specialSaleCache = res.products ?? [];
      })
    );
  }

  reviewSummary(productId: number): Observable<ReviewSummaryDto> {
    return this.http.get<ReviewSummaryDto>(`${API_BASE_URL}/products/${productId}/reviews/summary`);
  }

  reviews(productId: number, opts: { page?: number; size?: number } = {}): Observable<Page<ReviewDto>> {
    const params = new HttpParams()
      .set('page', String(opts.page ?? 0))
      .set('size', String(opts.size ?? 10));
    return this.http.get<Page<ReviewDto>>(`${API_BASE_URL}/products/${productId}/reviews`, { params });
  }

  createReview(
    productId: number,
    body: { rating: number; title?: string | null; comment?: string | null }
  ): Observable<ReviewDto> {
    return this.http.post<ReviewDto>(`${API_BASE_URL}/products/${productId}/reviews`, body);
  }
}
