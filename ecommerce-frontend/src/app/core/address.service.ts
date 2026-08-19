import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AddressDto } from './models';

@Injectable({ providedIn: 'root' })
export class AddressService {
  constructor(private readonly http: HttpClient) {}

  list(): Observable<AddressDto[]> {
    return this.http.get<AddressDto[]>(`${API_BASE_URL}/addresses`);
  }

  create(body: AddressDto): Observable<AddressDto> {
    return this.http.post<AddressDto>(`${API_BASE_URL}/addresses`, body);
  }

  update(id: number, body: AddressDto): Observable<AddressDto> {
    return this.http.put<AddressDto>(`${API_BASE_URL}/addresses/${id}`, body);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/addresses/${id}`);
  }
}
