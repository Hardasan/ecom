import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { OrderDto, PaymentInitiationDto } from './models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private readonly http: HttpClient) {}

  checkout(addressId: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/checkout`, { addressId });
  }

  list(): Observable<OrderDto[]> {
    return this.http.get<OrderDto[]>(`${API_BASE_URL}/orders`);
  }

  get(orderId: number): Observable<OrderDto> {
    return this.http.get<OrderDto>(`${API_BASE_URL}/orders/${orderId}`);
  }

  pay(orderId: number): Observable<PaymentInitiationDto> {
    return this.http.post<PaymentInitiationDto>(`${API_BASE_URL}/orders/${orderId}/pay`, {});
  }

  confirmPayment(orderId: number, paymentReference: string): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/payment/confirm`, {
      paymentReference
    });
  }
}
