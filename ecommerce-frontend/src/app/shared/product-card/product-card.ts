import { Component, computed, inject, input, output, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { WishlistService } from '../../core/wishlist.service';
import { CartItemDto, ProductDto } from '../../core/models';
import { displayName, priceParts, productImageSrc, tomanText, toFa, toNumber } from '../../core/format';
import { FaNumPipe } from '../../core/fa-num.pipe';

/**
 * Storefront product card (13-Shahrivar design): image well, wishlist heart, discount tag, title,
 * rating row, and a price-group beside a green add-to-cart button that becomes a qty stepper once
 * the line is in the cart. Shared by home / product-list / search / wishlist so the card is defined
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
    const p = this.product();
    this.cart
      .addItem({
        productId: p.id,
        quantity: 1,
        variantType: p.variantType,
        variantValue: this.variantValue()
      })
      .subscribe({
        next: () => this.notify.emit('به سبد خرید اضافه شد'),
        error: (err) => this.notify.emit(err?.error?.message ?? 'افزودن به سبد خرید انجام نشد. لطفاً دوباره تلاش کنید.')
      });
  }

  inc(event: Event, line: CartItemDto): void {
    event.preventDefault();
    event.stopPropagation();
    this.cart.increment(line.id).subscribe({
      error: (err) => this.notify.emit(err?.error?.message ?? 'تعداد به‌روزرسانی نشد. لطفاً دوباره تلاش کنید.')
    });
  }

  dec(event: Event, line: CartItemDto): void {
    event.preventDefault();
    event.stopPropagation();
    this.cart.decrement(line.id).subscribe({
      error: () => this.notify.emit('تعداد به‌روزرسانی نشد. لطفاً دوباره تلاش کنید.')
    });
  }
}
