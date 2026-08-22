import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CartDto, CartItemDto } from './models';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly cartSignal = signal<CartDto | null>(null);
  private readonly totalQuantitySignal = signal(0);

  /** The latest known cart, kept in sync on every cart mutation. */
  readonly cart = this.cartSignal.asReadonly();

  /** Total item count across the cart — drives the navbar cart badge. */
  readonly totalQuantity = this.totalQuantitySignal.asReadonly();

  /** Persian-formatted badge label, capped at ۹۹+. Empty when the cart is empty. */
  readonly badge = computed(() => {
    const n = this.totalQuantitySignal();
    if (n <= 0) {
      return '';
    }
    const shown = new Intl.NumberFormat('fa-IR').format(Math.min(n, 99));
    return n > 99 ? `${shown}+` : shown;
  });

  get(): Observable<CartDto> {
    return this.http.get<CartDto>(`${API_BASE_URL}/cart`).pipe(tap((c) => this.sync(c)));
  }

  addItem(body: {
    productId: number;
    quantity: number;
    variantType?: string | null;
    variantValue?: string | null;
  }): Observable<CartDto> {
    return this.http.post<CartDto>(`${API_BASE_URL}/cart/items`, body).pipe(tap((c) => this.sync(c)));
  }

  increment(itemId: number): Observable<CartDto> {
    return this.http
      .post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/increment`, {})
      .pipe(tap((c) => this.sync(c)));
  }

  decrement(itemId: number): Observable<CartDto> {
    return this.http
      .post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/decrement`, {})
      .pipe(tap((c) => this.sync(c)));
  }

  remove(itemId: number): Observable<CartDto> {
    return this.http
      .delete<CartDto>(`${API_BASE_URL}/cart/items/${itemId}`)
      .pipe(tap((c) => this.sync(c)));
  }

  /**
   * The cart line for a product (and the given variant, matching what a card adds),
   * or undefined when the product is not in the cart. Reactive to cart changes, so
   * templates can swap an "add" button for a quantity stepper.
   */
  lineFor(productId: number, variantValue?: string | null): CartItemDto | undefined {
    const target = variantValue ?? null;
    return (this.cartSignal()?.items ?? []).find(
      (i) => i.productId === productId && (i.variantValue ?? null) === target
    );
  }

  /** Reload the cart from the server; clears it when signed out. */
  refresh(): void {
    if (!this.auth.isLoggedIn()) {
      this.clear();
      return;
    }
    this.get().subscribe({ error: () => this.clear() });
  }

  /** Reset the cart locally (e.g. after checkout empties the server cart). */
  clear(): void {
    this.cartSignal.set(null);
    this.totalQuantitySignal.set(0);
  }

  private sync(cart: CartDto | null): void {
    this.cartSignal.set(cart);
    this.totalQuantitySignal.set(cart?.totalQuantity ?? 0);
  }
}
