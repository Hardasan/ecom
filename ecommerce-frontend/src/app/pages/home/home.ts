import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { AddressService } from '../../core/address.service';
import { CartService } from '../../core/cart.service';
import { CategoryService } from '../../core/category.service';
import { ConfigService } from '../../core/config.service';
import { ProductService } from '../../core/product.service';
import { ProductDto } from '../../core/models';
import { displayName, productImageSrc, toFa } from '../../core/format';
import { BottomNav } from '../../shared/bottom-nav/bottom-nav';
import { ProductCard } from '../../shared/product-card/product-card';

const CAT_TONES = ['kitchen', 'smart', 'other', 'electric', 'home'] as const;

@Component({
  selector: 'app-home',
  imports: [RouterLink, BottomNav, ProductCard],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home implements OnInit, OnDestroy {
  readonly a = ASSETS;
  readonly auth = inject(AuthService);
  readonly address = inject(AddressService);
  private readonly productsApi = inject(ProductService);
  private readonly categoriesApi = inject(CategoryService);
  readonly cartApi = inject(CartService);
  private readonly configApi = inject(ConfigService);

  readonly products = signal<ProductDto[]>([]);
  readonly categories = signal<{ id: number; name: string; tone: string; image: string }[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');

  // Flash-sale countdown to end of the day (HH:MM:SS, Persian), ticked every second.
  readonly countdown = signal('');
  private timer?: ReturnType<typeof setInterval>;

  // Address switcher sheet (opened from the "ارسال به" chip). Only relevant when signed in.
  readonly addressSheetOpen = signal(false);

  ngOnInit(): void {
    this.configApi.load().subscribe({ error: () => undefined });

    this.tickCountdown();
    this.timer = setInterval(() => this.tickCountdown(), 1000);

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
        this.error.set('ارتباط با سرور برقرار نشد. لطفاً دوباره تلاش کنید.');
        this.loading.set(false);
      }
    });
  }

  nameOf(p: ProductDto): string {
    return displayName(p);
  }

  imgOf(p: ProductDto): string {
    return productImageSrc(p);
  }

  heroProduct(): ProductDto | null {
    const sale = this.products();
    return sale.find((p) => p.url === 'converse-high-tops') ?? sale[0] ?? null;
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private tickCountdown(): void {
    const now = new Date();
    const end = new Date(now);
    end.setHours(23, 59, 59, 999);
    let s = Math.max(0, Math.floor((end.getTime() - now.getTime()) / 1000));
    const h = Math.floor(s / 3600);
    s %= 3600;
    const m = Math.floor(s / 60);
    const sec = s % 60;
    const pad = (n: number) => String(n).padStart(2, '0');
    this.countdown.set(toFa(`${pad(h)}:${pad(m)}:${pad(sec)}`));
  }

  showToast(msg: string): void {
    this.toast.set(msg);
    setTimeout(() => this.toast.set(''), 2500);
  }

  openAddressSheet(): void {
    this.address.refresh();
    this.addressSheetOpen.set(true);
  }

  chooseAddress(id: number | undefined): void {
    if (id != null) this.address.select(id);
    this.addressSheetOpen.set(false);
  }

  promoCode() {
    this.showToast('کد تخفیف: RIVANI40');
  }
}
