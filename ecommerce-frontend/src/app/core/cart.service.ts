import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, concat, map, of, switchMap, tap, throwError, toArray } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { CartDto, CartItemDto, ProductDto } from './models';
import { AuthService } from './auth.service';
import { ProductService } from './product.service';
import { toNumber } from './format';

const GUEST_CART_KEY = 'rivani_guest_cart';

/** A guest cart line: the add-time snapshot the server would otherwise keep, held client-side. */
type GuestLine = {
  id: number;
  productId: number;
  productName: string;
  productCode?: string;
  mainImage?: CartItemDto['mainImage'];
  variantType?: string | null;
  variantValue?: string | null;
  quantity: number;
  unitPrice: number;
  discountPrice: number | null;
  inventoryCount: number;
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly productsApi = inject(ProductService);

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
    if (!this.auth.isLoggedIn()) {
      return of(this.guestCartDto()).pipe(tap((c) => this.sync(c)));
    }
    return this.http.get<CartDto>(`${API_BASE_URL}/cart`).pipe(tap((c) => this.sync(c)));
  }

  addItem(body: {
    productId: number;
    quantity: number;
    variantType?: string | null;
    variantValue?: string | null;
  }): Observable<CartDto> {
    if (this.auth.isLoggedIn()) {
      return this.http.post<CartDto>(`${API_BASE_URL}/cart/items`, body).pipe(tap((c) => this.sync(c)));
    }
    // Guest: snapshot the product locally (mirrors the server's add-time snapshot) after re-validating
    // availability against the freshest catalog data, so the badge/cart stay accurate offline.
    return this.productsApi.getById(body.productId).pipe(
      switchMap((product) => {
        const error = this.guestAddError(product, body);
        if (error) {
          return throwError(() => ({ error: { message: error } }));
        }
        return of(this.addToGuestCart(product, body));
      })
    );
  }

  increment(itemId: number): Observable<CartDto> {
    if (this.auth.isLoggedIn()) {
      return this.http
        .post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/increment`, {})
        .pipe(tap((c) => this.sync(c)));
    }
    const lines = this.readGuest();
    const line = lines.find((l) => l.id === itemId);
    if (!line) {
      return throwError(() => ({ error: { message: 'کالای سبد یافت نشد' } }));
    }
    if (line.quantity + 1 > line.inventoryCount) {
      return throwError(() => ({ error: { message: 'موجودی کافی نیست' } }));
    }
    line.quantity += 1;
    return of(this.writeGuest(lines));
  }

  decrement(itemId: number): Observable<CartDto> {
    if (this.auth.isLoggedIn()) {
      return this.http
        .post<CartDto>(`${API_BASE_URL}/cart/items/${itemId}/decrement`, {})
        .pipe(tap((c) => this.sync(c)));
    }
    let lines = this.readGuest();
    const line = lines.find((l) => l.id === itemId);
    if (!line) {
      return throwError(() => ({ error: { message: 'کالای سبد یافت نشد' } }));
    }
    if (line.quantity <= 1) {
      lines = lines.filter((l) => l.id !== itemId);
    } else {
      line.quantity -= 1;
    }
    return of(this.writeGuest(lines));
  }

  remove(itemId: number): Observable<CartDto> {
    if (this.auth.isLoggedIn()) {
      return this.http
        .delete<CartDto>(`${API_BASE_URL}/cart/items/${itemId}`)
        .pipe(tap((c) => this.sync(c)));
    }
    const lines = this.readGuest().filter((l) => l.id !== itemId);
    return of(this.writeGuest(lines));
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

  /** Reload the cart from the current source (server when signed in, local guest cart otherwise). */
  refresh(): void {
    this.get().subscribe({ error: () => this.clear() });
  }

  /**
   * Called when the user signs in: replay the guest cart onto the server (so nothing is lost),
   * clear the local guest cart, then adopt the server cart as the source of truth. Completes when
   * the server cart is loaded, so the caller can wait before navigating (no pre-merge flash).
   */
  onLogin(): Observable<void> {
    const lines = this.readGuest();
    if (!lines.length) {
      return this.get().pipe(
        map(() => void 0),
        catchError(() => {
          this.clear();
          return of(void 0);
        })
      );
    }
    const adds = lines.map((l) =>
      this.http.post<CartDto>(`${API_BASE_URL}/cart/items`, {
        productId: l.productId,
        quantity: l.quantity,
        variantType: l.variantType,
        variantValue: l.variantValue
      })
    );
    // Replay sequentially so merges are deterministic; ignore per-item failures (e.g. now out of
    // stock) and always finish by loading the authoritative server cart.
    return concat(...adds).pipe(
      toArray(),
      switchMap(() => {
        this.clearGuestStorage();
        return this.get();
      }),
      map(() => void 0),
      catchError(() => {
        this.clearGuestStorage();
        this.clear();
        return of(void 0);
      })
    );
  }

  /** Reset the cart locally (e.g. after checkout empties the server cart). */
  clear(): void {
    this.cartSignal.set(null);
    this.totalQuantitySignal.set(0);
  }

  /** On sign-out, drop the local guest cart and empty the badge. */
  onLogout(): void {
    this.clearGuestStorage();
    this.clear();
  }

  private sync(cart: CartDto | null): void {
    this.cartSignal.set(cart);
    this.totalQuantitySignal.set(cart?.totalQuantity ?? 0);
  }

  // ---- Guest cart (localStorage) ----------------------------------------------------------------

  private readGuest(): GuestLine[] {
    try {
      const raw = localStorage.getItem(GUEST_CART_KEY);
      const parsed = raw ? (JSON.parse(raw) as GuestLine[]) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  /** Persist the guest lines, refresh the signals, and return the rebuilt cart DTO. */
  private writeGuest(lines: GuestLine[]): CartDto {
    try {
      localStorage.setItem(GUEST_CART_KEY, JSON.stringify(lines));
    } catch {
      // storage unavailable (private mode) — the in-memory signals still drive the UI this session
    }
    const dto = this.buildDto(lines);
    this.sync(dto);
    return dto;
  }

  private clearGuestStorage(): void {
    try {
      localStorage.removeItem(GUEST_CART_KEY);
    } catch {
      // ignore
    }
  }

  private guestCartDto(): CartDto {
    return this.buildDto(this.readGuest());
  }

  private guestAddError(
    product: ProductDto,
    body: { quantity: number; variantType?: string | null; variantValue?: string | null }
  ): string | null {
    if ((product.status ?? '').toUpperCase() !== 'ACTIVE') {
      return 'محصول در دسترس نیست';
    }
    if (product.variantType) {
      const exists = (product.prices ?? []).some(
        (p) => (p.variantValue ?? null) === (body.variantValue ?? null)
      );
      if (!exists) {
        return 'تنوع محصول یافت نشد';
      }
    }
    const existing = this.readGuest().find(
      (l) =>
        l.productId === product.id &&
        (l.variantType ?? null) === (body.variantType ?? null) &&
        (l.variantValue ?? null) === (body.variantValue ?? null)
    );
    const desired = (existing?.quantity ?? 0) + body.quantity;
    const stock = product.inventoryCount ?? 0;
    if (stock <= 0) {
      return 'محصول در دسترس نیست';
    }
    if (desired > stock) {
      return 'موجودی کافی نیست';
    }
    return null;
  }

  private addToGuestCart(
    product: ProductDto,
    body: { productId: number; quantity: number; variantType?: string | null; variantValue?: string | null }
  ): CartDto {
    const lines = this.readGuest();
    const price =
      (product.prices ?? []).find((p) => (p.variantValue ?? null) === (body.variantValue ?? null)) ??
      product.prices?.[0];
    const discount = price?.discountPrice != null ? toNumber(price.discountPrice) : 0;

    const existing = lines.find(
      (l) =>
        l.productId === product.id &&
        (l.variantType ?? null) === (body.variantType ?? null) &&
        (l.variantValue ?? null) === (body.variantValue ?? null)
    );
    if (existing) {
      existing.quantity += body.quantity;
      existing.inventoryCount = product.inventoryCount ?? existing.inventoryCount;
    } else {
      const nextId = lines.reduce((max, l) => Math.max(max, l.id), 0) + 1;
      lines.push({
        id: nextId,
        productId: product.id,
        productName: product.localName || product.name || 'محصول',
        productCode: product.code,
        mainImage: product.mainImage ?? null,
        variantType: body.variantType ?? product.variantType ?? null,
        variantValue: body.variantValue ?? null,
        quantity: body.quantity,
        unitPrice: toNumber(price?.price),
        discountPrice: discount > 0 ? discount : null,
        inventoryCount: product.inventoryCount ?? 0
      });
    }
    return this.writeGuest(lines);
  }

  private buildDto(lines: GuestLine[]): CartDto {
    const items: CartItemDto[] = lines.map((l) => {
      const effective = l.discountPrice != null && l.discountPrice > 0 ? l.discountPrice : l.unitPrice;
      return {
        id: l.id,
        productId: l.productId,
        productName: l.productName,
        productCode: l.productCode,
        mainImage: l.mainImage,
        variantType: l.variantType,
        variantValue: l.variantValue,
        quantity: l.quantity,
        unitPrice: l.unitPrice,
        discountPrice: l.discountPrice,
        effectivePrice: effective,
        lineTotal: effective * l.quantity
      };
    });
    return {
      userId: 0,
      items,
      totalQuantity: items.reduce((sum, i) => sum + i.quantity, 0),
      totalPrice: items.reduce((sum, i) => sum + toNumber(i.lineTotal), 0)
    };
  }
}
