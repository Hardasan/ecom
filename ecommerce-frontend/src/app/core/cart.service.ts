import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CartDto } from './models';

@Injectable({ providedIn: 'root' })
export class CartService {
  constructor(private readonly http: HttpClient) {}

  get(): Observable<CartDto> {
    return this.http.get<CartDto>(`${API_BASE_URL}/cart`);
  }

  addItem(body: {
    productId: number;
    quantity: number;
    variantType?: string | null;
    variantValue?: string | null;
  }): Observable<CartDto> {
    return this.http.post<CartDto>(`${API_BASE_URL}/cart/items`, body);
  }

  increment(itemId: number): Observable<CartDto> {
    return this.http.post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/increment`, {});
  }

  decrement(itemId: number): Observable<CartDto> {
    return this.http.post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/decrement`, {});
  }

  remove(itemId: number): Observable<CartDto> {
    return this.http.delete<CartDto>(`${API_BASE_URL}/cart/items/${itemId}`);
  }
}
