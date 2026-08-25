import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { OrderDto } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminOrderService {
  private readonly http = inject(HttpClient);

  list(): Observable<OrderDto[]> {
    return this.http.get<OrderDto[]>(`${API_BASE_URL}/admin/orders`);
  }

  refundable(): Observable<OrderDto[]> {
    return this.http.get<OrderDto[]>(`${API_BASE_URL}/admin/orders/refundable`);
  }

  get(id: number): Observable<OrderDto> {
    return this.http.get<OrderDto>(`${API_BASE_URL}/admin/orders/${id}`);
  }

  send(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/admin/orders/${id}/send`, {});
  }

  cancel(id: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/admin/orders/${id}/cancel`, {});
  }

  refund(id: number, body: { reference: string; iban: string }): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/admin/orders/${id}/refund`, body);
  }
}
