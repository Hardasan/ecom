import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { CreateStaffPayload, StaffDto } from '../../core/models';

/** Admin management of warehouse-staff accounts. */
@Injectable({ providedIn: 'root' })
export class AdminStaffService {
  private readonly http = inject(HttpClient);
  private readonly base = `${API_BASE_URL}/admin/staff`;

  list(): Observable<StaffDto[]> {
    return this.http.get<StaffDto[]>(this.base);
  }

  create(body: CreateStaffPayload): Observable<StaffDto> {
    return this.http.post<StaffDto>(this.base, body);
  }

  setStatus(id: number, enabled: boolean): Observable<StaffDto> {
    return this.http.patch<StaffDto>(`${this.base}/${id}/status`, { enabled });
  }

  resetPassword(id: number, password: string): Observable<void> {
    return this.http.post<void>(`${this.base}/${id}/reset-password`, { password });
  }
}
