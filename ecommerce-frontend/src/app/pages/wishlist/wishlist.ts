import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { AuthService } from '../../core/auth.service';
import { WishlistService } from '../../core/wishlist.service';
import { WishlistItemDto } from '../../core/models';
import { displayName, imageSrc, priceParts, tomanText } from '../../core/format';

/**
 * Wishlist / «علاقه‌مندی‌ها» — the shopper's saved products (reached from the profile stat card).
 * A bookmark is product-level (no variant), so cards link to the product page (to pick a variant and
 * add to cart) rather than quick-adding; the heart removes the bookmark. Out-of-stock lines are
 * deliberately kept (the whole point of a wishlist) and badged «ناموجود».
 */
@Component({
  selector: 'app-wishlist',
  imports: [RouterLink, FaNumPipe],
  templateUrl: './wishlist.html',
  styleUrl: './wishlist.scss'
})
export class WishlistPage implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly wishlistApi = inject(WishlistService);
  private readonly router = inject(Router);

  readonly items = signal<WishlistItemDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/wishlist' } });
      return;
    }
    this.wishlistApi.list().subscribe({
      next: (res) => {
        this.items.set(res.items ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('علاقه‌مندی‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  name(it: WishlistItemDto): string {
    return displayName({ localName: it.productLocalName, name: it.productName });
  }

  image(it: WishlistItemDto): string {
    return imageSrc(it.mainImage);
  }

  nowToman(it: WishlistItemDto): string {
    return tomanText(priceParts(it.prices).now);
  }

  wasToman(it: WishlistItemDto): string {
    const was = priceParts(it.prices).was;
    return was != null ? tomanText(was) : '';
  }

  percentOff(it: WishlistItemDto): number {
    return priceParts(it.prices).percentOff;
  }

  remove(it: WishlistItemDto, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    const prev = this.items();
    this.items.set(prev.filter((x) => x.productId !== it.productId)); // optimistic
    this.wishlistApi.removeByProduct(it.productId).subscribe({
      next: () => this.flash('از علاقه‌مندی‌ها حذف شد'),
      error: () => {
        this.items.set(prev); // roll back on failure
        this.flash('حذف ناموفق بود. لطفاً دوباره تلاش کنید.');
      }
    });
  }

  private flash(message: string): void {
    this.toast.set(message);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
