import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);

  contains(productId: number): Observable<{ inWishlist: boolean }> {
    return this.http.get<{ inWishlist: boolean }>(`${API_BASE_URL}/wishlist/products/${productId}`);
  }

  add(productId: number): Observable<unknown> {
    return this.http.post(`${API_BASE_URL}/wishlist/items`, { productId });
  }

  removeByProduct(productId: number): Observable<unknown> {
    return this.http.delete(`${API_BASE_URL}/wishlist/products/${productId}`);
  }
}
