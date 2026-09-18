import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { CartService } from '../../core/cart.service';
import { CartDto, CartItemDto } from '../../core/models';
import { formatPrice, imageSrc, toNumber, variantLabel, variantSwatch } from '../../core/format';
import { BottomNav } from '../../shared/bottom-nav/bottom-nav';

@Component({
  selector: 'app-cart',
  imports: [RouterLink, FaNumPipe, BottomNav],
  templateUrl: './cart.html',
  styleUrl: './cart.scss'
})
export class Cart implements OnInit {
  readonly a = ASSETS;
  private readonly cartApi = inject(CartService);
  private readonly router = inject(Router);

  readonly cart = signal<CartDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    // Guests can view and edit their (local) cart; sign-in is only required at checkout.
    this.reload();
  }

  reload() {
    this.loading.set(true);
    this.cartApi.get().subscribe({
      next: (c) => {
        this.cart.set(c);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('خطا در دریافت سبد');
        this.loading.set(false);
      }
    });
  }

  items(): CartItemDto[] {
    return this.cart()?.items ?? [];
  }

  total(): string {
    return formatPrice(this.cart()?.totalPrice);
  }

  linePrice(item: CartItemDto): string {
    return formatPrice(item.lineTotal);
  }

  unitPrice(item: CartItemDto): string {
    return formatPrice(item.effectivePrice);
  }

  imgOf(item: CartItemDto): string {
    return imageSrc(item.mainImage);
  }

  /** CSS color for a COLOR variant line's swatch dot, or '' when the variant is not a colour. */
  variantHex(item: CartItemDto): string {
    return variantSwatch(item.variantType, item.variantValue);
  }

  /** Readable variant label: Persian colour name for COLOR, raw value otherwise. */
  variantText(item: CartItemDto): string {
    return variantLabel(item.variantType, item.variantValue);
  }

  saving(): string {
    const items = this.items();
    let save = 0;
    for (const i of items) {
      const unit = toNumber(i.unitPrice);
      const effective = toNumber(i.effectivePrice);
      if (unit > effective) {
        save += (unit - effective) * i.quantity;
      }
    }
    return formatPrice(save);
  }

  inc(item: CartItemDto) {
    this.cartApi.increment(item.id).subscribe({
      next: (c) => this.cart.set(c),
      error: () => this.error.set('به‌روزرسانی تعداد ناموفق بود')
    });
  }

  dec(item: CartItemDto) {
    this.cartApi.decrement(item.id).subscribe({
      next: (c) => this.cart.set(c),
      error: () => this.error.set('به‌روزرسانی تعداد ناموفق بود')
    });
  }

  remove(item: CartItemDto) {
    this.cartApi.remove(item.id).subscribe({
      next: (c) => this.cart.set(c),
      error: () => this.error.set('حذف آیتم ناموفق بود')
    });
  }

  checkout() {
    if (!this.items().length) {
      return;
    }
    void this.router.navigateByUrl('/checkout');
  }
}
