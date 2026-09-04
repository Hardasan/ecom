import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CreateReturnBody, OrderDto, ReturnReason, ReturnRequestDto } from './models';

/** Persian labels for the return reasons (علت مرجوعی), shown in the picker + on requests. */
export const RETURN_REASONS: { value: ReturnReason; label: string }[] = [
  { value: 'SIZE_OR_COLOR_MISMATCH', label: 'عدم تطابق سایز یا رنگ' },
  { value: 'DEFECTIVE', label: 'کالای معیوب یا آسیب‌دیده' },
  { value: 'NOT_AS_DESCRIBED', label: 'مغایرت با مشخصات اعلام‌شده' },
  { value: 'CHANGED_MIND', label: 'انصراف از خرید' },
  { value: 'OTHER', label: 'سایر' }
];

export function returnReasonLabel(reason: ReturnReason): string {
  return RETURN_REASONS.find((r) => r.value === reason)?.label ?? 'سایر';
}

@Injectable({ providedIn: 'root' })
export class ReturnService {
  private readonly http = inject(HttpClient);

  /** Orders still eligible for return (RECEIVED, within the window, not already requested). */
  returnableOrders(): Observable<OrderDto[]> {
    return this.http.get<OrderDto[]>(`${API_BASE_URL}/returns/returnable-orders`);
  }

  list(): Observable<ReturnRequestDto[]> {
    return this.http.get<ReturnRequestDto[]>(`${API_BASE_URL}/returns`);
  }

  get(id: number): Observable<ReturnRequestDto> {
    return this.http.get<ReturnRequestDto>(`${API_BASE_URL}/returns/${id}`);
  }

  create(body: CreateReturnBody): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${API_BASE_URL}/returns`, body);
  }
}
