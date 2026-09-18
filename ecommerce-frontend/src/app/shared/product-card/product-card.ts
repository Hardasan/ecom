import { Component, computed, inject, input, output, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { WishlistService } from '../../core/wishlist.service';
import { CartItemDto, ProductDto } from '../../core/models';
import { displayName, priceParts, productImageSrc, tomanText, toFa, toNumber } from '../../core/format';
import { FaNumPipe } from '../../core/fa-num.pipe';

/**
 * Storefront product card (13-Shahrivar design): image well, wishlist heart, discount tag, title,
 * rating row and price-group. The green add-to-cart button sits on the photo's corner and grows into
 * a qty pill once the line is in the cart. Shared by home / product-list / search / wishlist so the card is defined
 * once. Cart + wishlist mutations are handled internally; a `notify` output lets the host page show
 * its own toast for feedback.
 */
@Component({
  selector: 'app-product-card',
  imports: [RouterLink, FaNumPipe],
  templateUrl: './product-card.html',
  styleUrl: './product-card.scss'
})
export class ProductCard {
  readonly product = input.required<ProductDto>();
  /** Hide the wishlist heart where it doesn't belong (e.g. inside the wishlist page itself). */
  readonly showFav = input(true);
  /** Emits user-facing feedback (Persian) for the host page's toast. */
  readonly notify = output<string>();

  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly wishlist = inject(WishlistService);
  private readonly router = inject(Router);

  readonly inWishlist = signal(false);
  readonly busy = signal(false);

  readonly name = computed(() => displayName(this.product()));
  readonly image = computed(() => productImageSrc(this.product()));
  readonly price = computed(() => priceParts(this.product().prices));
  readonly nowToman = computed(() => tomanText(this.price().now));
  readonly wasToman = computed(() => (this.price().was != null ? tomanText(this.price().was) : ''));

  readonly ratingCount = computed(() => this.product().ratingCount ?? 0);
  readonly ratingValue = computed(() => toFa(toNumber(this.product().averageRating).toFixed(1)));

  /** Default variant value for the "quick add" from a card (first price row). */
  private variantValue = computed(() => this.product().prices?.[0]?.variantValue ?? null);

  /** The cart line for this product's default variant, so the card can show a stepper. */
  readonly line = computed<CartItemDto | undefined>(() =>
    this.cart.lineFor(this.product().id, this.variantValue())
  );

  ngOnInit(): void {
    if (this.showFav() && this.auth.isLoggedIn()) {
      this.wishlist.contains(this.product().id).subscribe({
        next: (r) => this.inWishlist.set(!!r.inWishlist),
        error: () => undefined
      });
    }
  }

  toggleFav(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url } });
      return;
    }
    const id = this.product().id;
    if (this.inWishlist()) {
      this.inWishlist.set(false);
      this.wishlist.removeByProduct(id).subscribe({ error: () => this.inWishlist.set(true) });
    } else {
      this.inWishlist.set(true);
      this.wishlist.add(id).subscribe({ error: () => this.inWishlist.set(false) });
    }
  }

  add(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.busy()) {
      return;
    }
    const p = this.product();
    this.busy.set(true);
    this.cart
      .addItem({
        productId: p.id,
        quantity: 1,
        variantType: p.variantType,
        variantValue: this.variantValue()
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.notify.emit('به سبد خرید اضافه شد');
        },
        error: (err) => {
          this.busy.set(false);
          this.notify.emit(err?.error?.message ?? 'افزودن به سبد خرید انجام نشد. لطفاً دوباره تلاش کنید.');
        }
      });
  }

  inc(event: Event, line: CartItemDto): void {
    this.mutate(event, () => this.cart.increment(line.id));
  }

  dec(event: Event, line: CartItemDto): void {
    this.mutate(event, () => this.cart.decrement(line.id));
  }

  remove(event: Event, line: CartItemDto): void {
    this.mutate(event, () => this.cart.remove(line.id));
  }

  /**
   * One stepper request at a time, so a double-tap can't decrement a line of 2 straight out of the cart.
   * Takes a factory because the guest cart writes localStorage when the method is called, not on subscribe.
   */
  private mutate(event: Event, request: () => Observable<unknown>): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.busy()) {
      return;
    }
    this.busy.set(true);
    request().subscribe({
      next: () => this.busy.set(false),
      error: (err) => {
        this.busy.set(false);
        this.notify.emit(err?.error?.message ?? 'تعداد به‌روزرسانی نشد. لطفاً دوباره تلاش کنید.');
      }
    });
  }
}
