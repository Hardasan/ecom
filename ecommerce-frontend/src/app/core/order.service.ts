import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CheckoutQuoteDto, OrderDto, PaymentInitiationDto, PaymentMethod } from './models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private readonly http: HttpClient) {}

  /** Price preview (items + shipping) for the current cart to an address — no order is created. */
  quote(addressId: number): Observable<CheckoutQuoteDto> {
    return this.http.post<CheckoutQuoteDto>(`${API_BASE_URL}/checkout/quote`, { addressId });
  }

  checkout(addressId: number, paymentMethod: PaymentMethod = 'ONLINE'): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/checkout`, { addressId, paymentMethod });
  }

  cancel(orderId: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/cancel`, {});
  }

  receive(orderId: number): Observable<OrderDto> {
    return this.http.post<OrderDto>(`${API_BASE_URL}/orders/${orderId}/receive`, {});
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
