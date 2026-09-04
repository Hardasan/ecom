import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { AddressDto } from './models';
import { AuthService } from './auth.service';

const SELECTED_KEY = 'rivani_selected_address';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly auth = inject(AuthService);

  // Shared, reactive address state. The home "ارسال به" chip and the checkout address picker both
  // read `selected()` so the shopper's chosen delivery address is consistent across the app; the
  // choice survives reloads via localStorage. The CRUD methods below are unchanged (checkout still
  // calls list()/create()/… directly), so this is purely additive.
  private readonly addressesSignal = signal<AddressDto[]>([]);
  private readonly selectedIdSignal = signal<number | null>(restoreSelectedId());
  private loaded = false;

  readonly addresses = this.addressesSignal.asReadonly();

  /** The active delivery address: explicit selection → default → first, or null when none/guest. */
  readonly selected = computed<AddressDto | null>(() => {
    const list = this.addressesSignal();
    if (list.length === 0) return null;
    const id = this.selectedIdSignal();
    return (
      (id != null && list.find((a) => a.id === id)) ||
      list.find((a) => a.isDefault) ||
      list[0] ||
      null
    );
  });

  constructor(private readonly http: HttpClient) {
    // Load the address book whenever the shopper is (or becomes) logged in; clear it on sign-out so
    // the chip disappears immediately. Guests never have a server address book.
    effect(() => {
      if (this.auth.isLoggedIn()) {
        this.refresh();
      } else {
        this.loaded = false;
        this.addressesSignal.set([]);
      }
    });
  }

  /** (Re)load the address book from the server; safe to call repeatedly. */
  refresh(force = false): void {
    if (this.loaded && !force) return;
    this.loaded = true;
    this.list().subscribe({
      next: (list) => this.addressesSignal.set(list ?? []),
      error: () => {
        this.loaded = false;
        this.addressesSignal.set([]);
      }
    });
  }

  /** Pick the active delivery address (from the home chip / checkout picker). Persisted. */
  select(id: number | null): void {
    this.selectedIdSignal.set(id);
    try {
      if (id == null) localStorage.removeItem(SELECTED_KEY);
      else localStorage.setItem(SELECTED_KEY, String(id));
    } catch {
      /* storage may be unavailable (private mode) — selection just won't persist */
    }
  }

  list(): Observable<AddressDto[]> {
    return this.http.get<AddressDto[]>(`${API_BASE_URL}/addresses`);
  }

  create(body: AddressDto): Observable<AddressDto> {
    return this.http
      .post<AddressDto>(`${API_BASE_URL}/addresses`, body)
      .pipe(tap(() => this.refresh(true)));
  }

  update(id: number, body: AddressDto): Observable<AddressDto> {
    return this.http
      .put<AddressDto>(`${API_BASE_URL}/addresses/${id}`, body)
      .pipe(tap(() => this.refresh(true)));
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/addresses/${id}`).pipe(
      tap(() => {
        if (this.selectedIdSignal() === id) this.select(null);
        this.refresh(true);
      })
    );
  }
}

function restoreSelectedId(): number | null {
  try {
    const raw = localStorage.getItem(SELECTED_KEY);
    return raw ? Number(raw) : null;
  } catch {
    return null;
  }
}
