import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { AdminStatsDto } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminStatsService {
  private readonly http = inject(HttpClient);

  stats(): Observable<AdminStatsDto> {
    return this.http.get<AdminStatsDto>(`${API_BASE_URL}/admin/stats`);
  }
}
