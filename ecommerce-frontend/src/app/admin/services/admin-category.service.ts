import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { CategoryDto, CategoryHierarchyItem } from '../../core/models';

type CategoryBody = { name: string; localName?: string };

@Injectable({ providedIn: 'root' })
export class AdminCategoryService {
  private readonly http = inject(HttpClient);

  /** Uncached, unlike the storefront CategoryService — the admin must see edits immediately. */
  hierarchy(): Observable<{ categories: CategoryHierarchyItem[] }> {
    return this.http.get<{ categories: CategoryHierarchyItem[] }>(`${API_BASE_URL}/categories/hierarchy`);
  }

  create(body: CategoryBody): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${API_BASE_URL}/categories`, body);
  }

  createSub(parentId: number, body: CategoryBody): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${API_BASE_URL}/categories/${parentId}/subcategories`, body);
  }

  update(id: number, body: CategoryBody & { parentId?: number | null }): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${API_BASE_URL}/categories/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/categories/${id}`);
  }
}
