import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CategoryDto, CategoryHierarchyItem } from './models';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private listCache: CategoryDto[] | null = null;
  private hierarchyCache: CategoryHierarchyItem[] | null = null;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<{ categories: CategoryDto[] }> {
    if (this.listCache) {
      return of({ categories: this.listCache });
    }
    return this.http.get<{ categories: CategoryDto[] }>(`${API_BASE_URL}/categories`).pipe(
      tap((res) => {
        this.listCache = res.categories ?? [];
      })
    );
  }

  hierarchy(): Observable<{ categories: CategoryHierarchyItem[] }> {
    if (this.hierarchyCache) {
      return of({ categories: this.hierarchyCache });
    }
    return this.http
      .get<{ categories: CategoryHierarchyItem[] }>(`${API_BASE_URL}/categories/hierarchy`)
      .pipe(
        tap((res) => {
          this.hierarchyCache = res.categories ?? [];
        })
      );
  }
}
