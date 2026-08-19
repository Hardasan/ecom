import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { ClientConfigDto } from './models';

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private readonly config = signal<ClientConfigDto | null>(null);

  readonly otpTtlSeconds = computed(() => this.config()?.otpTtlSeconds ?? 120);

  constructor(private readonly http: HttpClient) {}

  load(): Observable<ClientConfigDto> {
    const cached = this.config();
    if (cached) {
      return of(cached);
    }
    return this.http
      .get<ClientConfigDto>(`${API_BASE_URL}/client-config`)
      .pipe(tap((config) => this.config.set(config)));
  }
}
