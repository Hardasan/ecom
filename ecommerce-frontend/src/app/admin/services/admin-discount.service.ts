import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { DiscountDto } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminDiscountService {
  private readonly http = inject(HttpClient);

  list(): Observable<{ discounts: DiscountDto[] }> {
    return this.http.get<{ discounts: DiscountDto[] }>(`${API_BASE_URL}/discounts`);
  }

  get(id: number): Observable<DiscountDto> {
    return this.http.get<DiscountDto>(`${API_BASE_URL}/discounts/${id}`);
  }

  create(body: DiscountDto): Observable<DiscountDto> {
    return this.http.post<DiscountDto>(`${API_BASE_URL}/discounts`, body);
  }

  update(id: number, body: DiscountDto): Observable<DiscountDto> {
    return this.http.put<DiscountDto>(`${API_BASE_URL}/discounts/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/discounts/${id}`);
  }
}
