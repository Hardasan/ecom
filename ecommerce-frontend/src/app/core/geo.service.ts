import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { GeoCityDto, GeoProvinceDto } from './models';

const STORAGE_KEY = 'geo-cache';

type GeoCache = {
  provinces: GeoProvinceDto[] | null;
  cities: Record<string, GeoCityDto[]>;
};

@Injectable({ providedIn: 'root' })
export class GeoService {
  private cache: GeoCache = this.readCache();

  constructor(private readonly http: HttpClient) {}

  listProvinces(): Observable<{ provinces: GeoProvinceDto[] }> {
    if (this.cache.provinces) {
      return of({ provinces: this.cache.provinces });
    }
    return this.http.get<{ provinces: GeoProvinceDto[] }>(`${API_BASE_URL}/geo/provinces`).pipe(
      tap((res) => {
        this.cache.provinces = res.provinces ?? [];
        this.writeCache();
      })
    );
  }

  listCities(province: string): Observable<{ cities: GeoCityDto[] }> {
    const cached = this.cache.cities[province];
    if (cached) {
      return of({ cities: cached });
    }
    return this.http.get<{ cities: GeoCityDto[] }>(`${API_BASE_URL}/geo/cities`, {
      params: { province }
    }).pipe(
      tap((res) => {
        this.cache.cities[province] = res.cities ?? [];
        this.writeCache();
      })
    );
  }

  private readCache(): GeoCache {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as GeoCache;
        return {
          provinces: parsed.provinces ?? null,
          cities: parsed.cities ?? {}
        };
      }
    } catch {
      localStorage.removeItem(STORAGE_KEY);
    }
    return { provinces: null, cities: {} };
  }

  private writeCache(): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(this.cache));
  }
}
