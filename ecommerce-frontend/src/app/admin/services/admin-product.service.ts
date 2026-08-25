import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../api.config';
import { BatchUploadResultDto, ImageType, ProductDto, ProductWriteDto } from '../../core/models';

@Injectable({ providedIn: 'root' })
export class AdminProductService {
  private readonly http = inject(HttpClient);

  /** POST /api/products — multipart: JSON `data` part + optional main image; `altText` is a query param. */
  create(data: ProductWriteDto, image?: File | null, altText?: string): Observable<ProductDto> {
    const form = new FormData();
    form.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
      form.append('image', image);
    }
    let params = new HttpParams();
    if (altText) {
      params = params.set('altText', altText);
    }
    return this.http.post<ProductDto>(`${API_BASE_URL}/products`, form, { params });
  }

  update(id: number, data: ProductWriteDto): Observable<ProductDto> {
    return this.http.put<ProductDto>(`${API_BASE_URL}/products/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/products/${id}`);
  }

  uploadImage(id: number, type: ImageType, image: File, altText?: string): Observable<ProductDto> {
    const form = new FormData();
    form.append('image', image);
    let params = new HttpParams().set('type', type);
    if (altText) {
      params = params.set('altText', altText);
    }
    return this.http.post<ProductDto>(`${API_BASE_URL}/products/${id}/images`, form, { params });
  }

  removeImage(id: number, type: ImageType, imageId?: number): Observable<void> {
    let params = new HttpParams().set('type', type);
    if (imageId != null) {
      params = params.set('imageId', String(imageId));
    }
    return this.http.delete<void>(`${API_BASE_URL}/products/${id}/images`, { params });
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get(`${API_BASE_URL}/products/template`, { responseType: 'blob' });
  }

  uploadBatch(file: File): Observable<BatchUploadResultDto> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<BatchUploadResultDto>(`${API_BASE_URL}/products/upload`, form);
  }
}
