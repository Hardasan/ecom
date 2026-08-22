import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { CategoryService } from '../../core/category.service';
import { ConfigService } from '../../core/config.service';
import { ProductService } from '../../core/product.service';
import { CartItemDto, ProductDto } from '../../core/models';
import { displayName, effectiveUnitPrice, formatPrice, productImageSrc } from '../../core/format';

const CAT_TONES = ['kitchen', 'smart', 'other', 'electric', 'home'] as const;

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit {
  readonly a = ASSETS;
  readonly auth = inject(AuthService);
  private readonly productsApi = inject(ProductService);
  private readonly categoriesApi = inject(CategoryService);
  readonly cartApi = inject(CartService);
  private readonly configApi = inject(ConfigService);
  private readonly router = inject(Router);

  readonly products = signal<ProductDto[]>([]);
  readonly categories = signal<{ id: number; name: string; tone: string; image: string }[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');

  ngOnInit(): void {
    this.configApi.load().subscribe({ error: () => undefined });

    this.productsApi.specialSale().subscribe({
      next: (res) => {
        const sale = res.products ?? [];
        this.products.set(sale);
        this.loading.set(false);
        this.categoriesApi.list().subscribe({
          next: (catRes) => {
            const roots = (catRes.categories ?? []).filter((c) => !c.parentId).slice(0, 4);
            this.categories.set(
              roots.map((c, i) => {
                const match = sale.find((p) => p.categoryId === c.id || p.subCategoryId === c.id);
                return {
                  id: c.id,
                  name: c.localName || c.name,
                  tone: CAT_TONES[i % CAT_TONES.length],
                  image: match ? productImageSrc(match) : ''
                };
              })
            );
          },
          error: () => undefined
        });
      },
      error: () => {
        this.error.set('ارتباط با سرور برقرار نشد. بک‌اند را روی پورت ۸۰۸۱ اجرا کنید.');
        this.loading.set(false);
      }
    });
  }

  nameOf(p: ProductDto): string {
    return displayName(p);
  }

  priceOf(p: ProductDto): string {
    return formatPrice(effectiveUnitPrice(p.prices));
  }

  imgOf(p: ProductDto): string {
    return productImageSrc(p);
  }

  heroProduct(): ProductDto | null {
    const sale = this.products();
    return sale.find((p) => p.url === 'converse-high-tops') ?? sale[0] ?? null;
  }

  addToCart(event: Event, p: ProductDto) {
    event.preventDefault();
    event.stopPropagation();
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/product/${p.id}` }
      });
      return;
    }
    this.cartApi
      .addItem({
        productId: p.id,
        quantity: 1,
        variantType: p.variantType,
        variantValue: p.prices?.[0]?.variantValue ?? null
      })
      .subscribe({
        next: () => {
          this.toast.set('به سبد اضافه شد');
          setTimeout(() => this.toast.set(''), 2000);
        },
        error: (err) => {
          this.toast.set(err?.error?.message ?? 'افزودن به سبد ناموفق بود');
          setTimeout(() => this.toast.set(''), 3000);
        }
      });
  }

  /** The cart line for a card's product+default variant, so the card can show a stepper. */
  cartLine(p: ProductDto): CartItemDto | undefined {
    return this.cartApi.lineFor(p.id, p.prices?.[0]?.variantValue ?? null);
  }

  faNum(n: number): string {
    return new Intl.NumberFormat('fa-IR').format(n);
  }

  inc(event: Event, line: CartItemDto) {
    event.preventDefault();
    event.stopPropagation();
    this.cartApi.increment(line.id).subscribe({
      error: (err) => {
        this.toast.set(err?.error?.message ?? 'به‌روزرسانی تعداد ناموفق بود');
        setTimeout(() => this.toast.set(''), 2500);
      }
    });
  }

  dec(event: Event, line: CartItemDto) {
    event.preventDefault();
    event.stopPropagation();
    this.cartApi.decrement(line.id).subscribe({
      error: () => {
        this.toast.set('به‌روزرسانی تعداد ناموفق بود');
        setTimeout(() => this.toast.set(''), 2500);
      }
    });
  }

  promoCode() {
    this.toast.set('کد تخفیف: RIVANI40');
    setTimeout(() => this.toast.set(''), 3000);
  }
}
