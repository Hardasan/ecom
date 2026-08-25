import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/product.service';
import { AdminProductService } from '../services/admin-product.service';
import { BatchUploadResultDto, ProductDto } from '../../core/models';
import { displayName, effectiveUnitPrice, formatPrice, imageSrc } from '../../core/format';

@Component({
  selector: 'app-admin-product-list',
  imports: [FormsModule, RouterLink],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss'
})
export class ProductListAdmin implements OnInit {
  private readonly productApi = inject(ProductService);
  private readonly adminApi = inject(AdminProductService);

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly toast = signal('');

  readonly showBulk = signal(false);
  readonly bulkResult = signal<BatchUploadResultDto | null>(null);
  readonly bulkBusy = signal(false);

  query = '';
  readonly img = imageSrc;
  readonly name = displayName;

  ngOnInit(): void {
    this.load(0);
  }

  load(page: number): void {
    this.loading.set(true);
    this.error.set('');
    this.productApi.search({ page, size: 20, localName: this.query || undefined }).subscribe({
      next: (res) => {
        this.products.set(res.content ?? []);
        this.page.set(res.number ?? 0);
        this.totalPages.set(res.totalPages ?? 0);
        this.totalElements.set(res.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('محصولات خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  search(): void {
    this.load(0);
  }

  price(p: ProductDto): string {
    return formatPrice(effectiveUnitPrice(p.prices));
  }

  stock(p: ProductDto): number {
    return p.inventoryCount ?? 0;
  }

  remove(p: ProductDto): void {
    if (!confirm(`حذف «${this.name(p)}»؟ این عملیات بازگشت‌پذیر نیست.`)) {
      return;
    }
    this.adminApi.delete(p.id).subscribe({
      next: () => {
        this.flash('محصول حذف شد');
        this.load(this.page());
      },
      error: (e) => this.error.set(e?.error?.message ?? 'حذف ناموفق بود')
    });
  }

  downloadTemplate(): void {
    this.adminApi.downloadTemplate().subscribe({
      next: (blob) => this.saveBlob(blob, 'product-template.xlsx'),
      error: () => this.error.set('دانلود قالب ناموفق بود')
    });
  }

  onBulkFile(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.bulkBusy.set(true);
    this.bulkResult.set(null);
    this.error.set('');
    this.adminApi.uploadBatch(file).subscribe({
      next: (res) => {
        this.bulkResult.set(res);
        this.bulkBusy.set(false);
        this.load(0);
      },
      error: (e) => {
        this.error.set(e?.error?.message ?? 'بارگذاری ناموفق بود');
        this.bulkBusy.set(false);
      }
    });
    input.value = '';
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }

  private flash(msg: string): void {
    this.toast.set(msg);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
