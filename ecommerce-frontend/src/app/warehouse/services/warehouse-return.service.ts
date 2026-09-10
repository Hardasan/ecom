import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { ReturnRequestDto, ReturnStatus } from '../../core/models';

/**
 * Warehouse returns console API. Staff receive an APPROVED return's goods and accept them (restocks
 * + moves it to RECEIVED so the admin can refund) or reject them at inspection.
 */
@Injectable({ providedIn: 'root' })
export class WarehouseReturnService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/warehouse/returns`;

  list(status: ReturnStatus | null): Observable<ReturnRequestDto[]> {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<ReturnRequestDto[]>(this.base, { params });
  }

  accept(id: number): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${this.base}/${id}/accept`, {});
  }

  reject(id: number): Observable<ReturnRequestDto> {
    return this.http.post<ReturnRequestDto>(`${this.base}/${id}/reject`, {});
  }
}
