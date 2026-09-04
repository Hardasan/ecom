import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { ProductService } from '../../core/product.service';
import { ProductDto } from '../../core/models';
import { ProductCard } from '../../shared/product-card/product-card';

@Component({
  selector: 'app-product-list',
  imports: [RouterLink, ProductCard],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductList implements OnInit {
  readonly a = ASSETS;
  private readonly route = inject(ActivatedRoute);
  private readonly productsApi = inject(ProductService);

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly title = signal('محصولات');
  readonly toast = signal('');
  query = '';

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const categoryId = params.get('categoryId');
      const subCategoryId = params.get('subCategoryId');
      const q = params.get('q') ?? '';
      this.query = q;
      this.title.set(params.get('title') || (q ? `جستجو: ${q}` : 'محصولات'));
      this.load(
        categoryId ? Number(categoryId) : undefined,
        subCategoryId ? Number(subCategoryId) : undefined,
        q || undefined
      );
    });
  }

  load(categoryId?: number, subCategoryId?: number, localName?: string) {
    this.loading.set(true);
    this.error.set('');
    this.productsApi
      .search({ size: 50, categoryId, subCategoryId, isAvailable: true, localName })
      .subscribe({
        next: (page) => {
          this.products.set(page.content ?? []);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('خطا در دریافت محصولات — آیا بک‌اند روشن است؟');
          this.loading.set(false);
        }
      });
  }

  showToast(msg: string): void {
    this.toast.set(msg);
    setTimeout(() => this.toast.set(''), 2500);
  }
}
