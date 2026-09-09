import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { ReturnRequestDto, ReturnStatus } from '../../core/models';

/** Admin returns (مرجوعی) moderation: review the queue, then approve / reject / refund. */
@Injectable({ providedIn: 'root' })
export class AdminReturnService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/admin/returns`;

  list(status: ReturnStatus | null): Observable<ReturnRequestDto[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ReturnRequestDto[]>(this.base, { params });
  }

  approve(id: number): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${this.base}/${id}/approve`, {});
  }

  reject(id: number): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${this.base}/${id}/reject`, {});
  }

  refund(id: number, body: { reference: string; iban?: string }): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${this.base}/${id}/refund`, body);
  }
}
