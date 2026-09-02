import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { OrderDto } from '../../core/models';

/** Warehouse fulfillment console API (approve -> ship -> deliver, or cancel). */
@Injectable({ providedIn: 'root' })
export class WarehouseOrderService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/warehouse/orders`;

  list(): Observable<OrderDto[]> {
    return this.http.get<OrderDto[]>(this.base);
  }

  get(id: number): Observable<OrderDto> {
    return this.http.get<OrderDto>(`${this.base}/${id}`);
  }

  approve(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${this.base}/${id}/approve`, {});
  }

  ship(id: number, body: { carrier: string; trackingNumber: string }): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${this.base}/${id}/ship`, body);
  }

  deliver(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${this.base}/${id}/deliver`, {});
  }

  cancel(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${this.base}/${id}/cancel`, {});
  }
}
