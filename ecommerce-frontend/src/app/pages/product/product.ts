import { Component, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { ProductService } from '../../core/product.service';
import { PriceDto, ProductDto, ReviewSummaryDto } from '../../core/models';
import { displayName, formatPrice, productImageSrc, toNumber } from '../../core/format';

type TabKey = 'desc' | 'spec' | 'reviews';

@Component({
  selector: 'app-product',
  imports: [RouterLink],
  templateUrl: './product.html',
  styleUrl: './product.scss'
})
export class Product implements OnInit {
  readonly a = ASSETS;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly productsApi = inject(ProductService);
  readonly cartApi = inject(CartService);

  readonly product = signal<ProductDto | null>(null);
  readonly summary = signal<ReviewSummaryDto | null>(null);
  readonly selectedVariant = signal<string | null>(null);
  readonly activeTab = signal<TabKey>('desc');
  readonly loading = signal(true);
  readonly buying = signal(false);
  readonly error = signal('');
  readonly toast = signal('');

  private readonly scrollArea = viewChild<ElementRef<HTMLElement>>('scrollArea');
  private readonly tabsBar = viewChild<ElementRef<HTMLElement>>('tabsBar');
  private readonly descSection = viewChild<ElementRef<HTMLElement>>('descSection');
  private readonly specSection = viewChild<ElementRef<HTMLElement>>('specSection');
  private readonly reviewsSection = viewChild<ElementRef<HTMLElement>>('reviewsSection');
  private scrollScheduled = false;

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = Number(params.get('id'));
      this.loadProduct(id);
    });
  }

  private loadProduct(id: number) {
    if (!id) {
      this.product.set(null);
      this.error.set('محصول نامعتبر است');
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.product.set(null);
    this.summary.set(null);
    this.activeTab.set('desc');

    this.productsApi.getById(id).subscribe({
      next: (p) => {
        this.product.set(p);
        const first = p.prices?.[0]?.variantValue ?? null;
        this.selectedVariant.set(first);
        this.loading.set(false);
      },
      error: () => {
        this.product.set(null);
        this.error.set('محصول پیدا نشد');
        this.loading.set(false);
      }
    });

    this.productsApi.reviewSummary(id).subscribe({
      next: (s) => this.summary.set(s),
      error: () => undefined
    });
  }

  name(): string {
    return this.product() ? displayName(this.product()!) : '';
  }

  image(): string {
    return productImageSrc(this.product());
  }

  variants(): PriceDto[] {
    return this.product()?.prices ?? [];
  }

  selectedPrice(): PriceDto | undefined {
    const prices = this.variants();
    if (!prices.length) {
      return undefined;
    }
    const selected = this.selectedVariant();
    return prices.find((p) => p.variantValue === selected) ?? prices[0];
  }

  currentPrice(): string {
    const p = this.selectedPrice();
    const value = p?.discountPrice != null && toNumber(p.discountPrice) > 0 ? p.discountPrice : p?.price;
    return formatPrice(value);
  }

  oldPrice(): string | null {
    const p = this.selectedPrice();
    if (!p?.discountPrice || toNumber(p.discountPrice) <= 0) {
      return null;
    }
    return formatPrice(p.price).replace(' تومان', '');
  }

  discountPercent(): string | null {
    const p = this.selectedPrice();
    if (!p?.discountPrice) {
      return null;
    }
    const price = toNumber(p.price);
    const discount = toNumber(p.discountPrice);
    if (price <= 0 || discount <= 0 || discount >= price) {
      return null;
    }
    return `${Math.round(((price - discount) / price) * 100)}٪`;
  }

  ratingText(): string {
    const s = this.summary();
    return s ? String(s.averageRating) : '—';
  }

  reviewCount(): string {
    const s = this.summary();
    return s ? `(${new Intl.NumberFormat('fa-IR').format(s.totalCount)} نظر)` : '';
  }

  specEntries(): { key: string; value: string }[] {
    const spec = this.product()?.specification;
    if (!spec) {
      return [];
    }
    return Object.entries(spec).map(([key, value]) => ({ key, value }));
  }

  selectVariant(value: string | null | undefined) {
    this.selectedVariant.set(value ?? null);
  }

  private sectionEl(tab: TabKey): HTMLElement | undefined {
    const map: Record<TabKey, ElementRef<HTMLElement> | undefined> = {
      desc: this.descSection(),
      spec: this.specSection(),
      reviews: this.reviewsSection()
    };
    return map[tab]?.nativeElement;
  }

  /** Click a tab → smooth-scroll its section just below the pinned tab bar. */
  goToTab(tab: TabKey) {
    const container = this.scrollArea()?.nativeElement;
    const target = this.sectionEl(tab);
    if (!container || !target) {
      return;
    }
    const barHeight = this.tabsBar()?.nativeElement.offsetHeight ?? 0;
    const top =
      target.getBoundingClientRect().top -
      container.getBoundingClientRect().top +
      container.scrollTop -
      barHeight -
      8;
    container.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    this.activeTab.set(tab);
  }

  /** Scroll-spy: rAF-throttled so a scroll storm collapses into one measure. */
  onSectionsScroll() {
    if (this.scrollScheduled) {
      return;
    }
    this.scrollScheduled = true;
    requestAnimationFrame(() => {
      this.scrollScheduled = false;
      this.updateActiveTab();
    });
  }

  private updateActiveTab() {
    const container = this.scrollArea()?.nativeElement;
    if (!container) {
      return;
    }
    // At the very bottom the last section may be too short to reach the line — force it active.
    if (container.scrollTop + container.clientHeight >= container.scrollHeight - 4) {
      this.activeTab.set('reviews');
      return;
    }
    const barHeight = this.tabsBar()?.nativeElement.offsetHeight ?? 0;
    const line = container.getBoundingClientRect().top + barHeight + 8;
    const order: TabKey[] = ['desc', 'spec', 'reviews'];
    let current: TabKey = 'desc';
    for (const tab of order) {
      const el = this.sectionEl(tab);
      if (el && el.getBoundingClientRect().top <= line) {
        current = tab;
      }
    }
    this.activeTab.set(current);
  }

  buy() {
    const p = this.product();
    if (!p || this.buying()) {
      return;
    }
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/product/${p.id}` }
      });
      return;
    }

    this.buying.set(true);
    this.error.set('');
    this.cartApi
      .addItem({
        productId: p.id,
        quantity: 1,
        variantType: p.variantType,
        variantValue: this.selectedVariant()
      })
      .subscribe({
        next: () => {
          this.buying.set(false);
          this.flash('به سبد اضافه شد');
        },
        error: (err) => {
          this.buying.set(false);
          this.error.set(err?.error?.message ?? 'افزودن به سبد ناموفق بود');
        }
      });
  }

  private flash(message: string) {
    this.toast.set(message);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
