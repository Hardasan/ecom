import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductService } from '../../core/product.service';
import { AdminProductService } from '../services/admin-product.service';
import { AdminCategoryService } from '../services/admin-category.service';
import {
  CategoryDto,
  CategoryHierarchyItem,
  ImageType,
  InventoryStatus,
  PriceDto,
  ProductDto,
  ProductStatus,
  ProductWriteDto,
  SPEC_KEYS,
  SpecificationKey,
  VariantTypeValue
} from '../../core/models';
import { imageSrc } from '../../core/format';

type PriceRow = { variantValue: string; price: number | null; discountPrice: number | null };
type SpecRow = { key: SpecificationKey; value: string };

@Component({
  selector: 'app-admin-product-form',
  imports: [FormsModule, RouterLink],
  templateUrl: './product-form.html',
  styleUrl: './product-form.scss'
})
export class ProductForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productApi = inject(ProductService);
  private readonly adminApi = inject(AdminProductService);
  private readonly categoryApi = inject(AdminCategoryService);

  readonly editId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');

  readonly categories = signal<CategoryHierarchyItem[]>([]);
  readonly product = signal<ProductDto | null>(null);
  readonly prices = signal<PriceRow[]>([{ variantValue: '', price: null, discountPrice: null }]);
  readonly specs = signal<SpecRow[]>([]);

  readonly specKeys = SPEC_KEYS;
  readonly statuses: ProductStatus[] = ['ACTIVE', 'INACTIVE'];
  readonly inventoryStatuses: InventoryStatus[] = ['IN_STOCK', 'LOW_STOCK', 'OUT_OF_STOCK'];
  readonly variantTypes: (VariantTypeValue | '')[] = ['', 'COLOR', 'SIZE'];
  readonly img = imageSrc;

  name = '';
  localName = '';
  url = '';
  categoryId: number | null = null;
  subCategoryId: number | null = null;
  brandId: number | null = null;
  variantType: VariantTypeValue | '' = '';
  status: ProductStatus = 'ACTIVE';
  inventoryStatus: InventoryStatus = 'IN_STOCK';
  inventoryCount: number | null = 0;
  weightGram: number | null = null;
  shortDescription = '';
  fullDescription = '';

  mainImageFile: File | null = null;
  altText = '';

  ngOnInit(): void {
    this.categoryApi.hierarchy().subscribe({
      next: (r) => this.categories.set(r.categories ?? []),
      error: () => undefined
    });
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editId.set(Number(idParam));
      this.loadProduct(Number(idParam));
    }
  }

  get isEdit(): boolean {
    return this.editId() !== null;
  }

  subCategories(): CategoryDto[] {
    return this.categories().find((c) => c.category.id === this.categoryId)?.subCategories ?? [];
  }

  onCategoryChange(): void {
    this.subCategoryId = null;
  }

  loadProduct(id: number): void {
    this.loading.set(true);
    this.productApi.getById(id).subscribe({
      next: (p) => {
        this.fill(p);
        this.product.set(p);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('محصول یافت نشد');
        this.loading.set(false);
      }
    });
  }

  addPrice(): void {
    this.prices.update((r) => [...r, { variantValue: '', price: null, discountPrice: null }]);
  }

  removePrice(i: number): void {
    this.prices.update((r) => (r.length > 1 ? r.filter((_, idx) => idx !== i) : r));
  }

  addSpec(): void {
    this.specs.update((r) => [...r, { key: this.specKeys[0], value: '' }]);
  }

  removeSpec(i: number): void {
    this.specs.update((r) => r.filter((_, idx) => idx !== i));
  }

  onMainImage(ev: Event): void {
    this.mainImageFile = (ev.target as HTMLInputElement).files?.[0] ?? null;
  }

  save(): void {
    const payload = this.buildPayload();
    if (!payload) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    const req = this.isEdit
      ? this.adminApi.update(this.editId()!, payload)
      : this.adminApi.create(payload, this.mainImageFile, this.altText || undefined);
    req.subscribe({
      next: (p) => {
        this.busy.set(false);
        if (this.isEdit) {
          this.product.set(p);
          this.flash('تغییرات ذخیره شد');
        } else {
          void this.router.navigate(['/admin/products', p.id]);
        }
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'ذخیره ناموفق بود');
      }
    });
  }

  uploadImage(type: ImageType, ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.isEdit) {
      return;
    }
    this.adminApi.uploadImage(this.editId()!, type, file).subscribe({
      next: (p) => {
        this.product.set(p);
        this.flash('تصویر افزوده شد');
      },
      error: (e) => this.error.set(e?.error?.message ?? 'بارگذاری تصویر ناموفق بود')
    });
    input.value = '';
  }

  removeImage(type: ImageType, imageId?: number): void {
    if (!this.isEdit) {
      return;
    }
    this.adminApi.removeImage(this.editId()!, type, imageId).subscribe({
      next: () => {
        this.refreshImages();
        this.flash('تصویر حذف شد');
      },
      error: (e) => this.error.set(e?.error?.message ?? 'حذف تصویر ناموفق بود')
    });
  }

  private buildPayload(): ProductWriteDto | null {
    if (!this.name.trim() || !this.url.trim() || this.categoryId == null || this.inventoryCount == null) {
      this.error.set('نام، شناسه (url)، دسته و موجودی الزامی است');
      return null;
    }
    const prices: PriceDto[] = this.prices()
      .filter((r) => r.price != null)
      .map((r) => ({
        variantValue: r.variantValue.trim() || null,
        price: Math.round((r.price as number) * 10),
        discountPrice: r.discountPrice != null ? Math.round(r.discountPrice * 10) : null
      }));
    if (!prices.length) {
      this.error.set('حداقل یک قیمت لازم است');
      return null;
    }
    const specification: Record<string, string> = {};
    for (const s of this.specs()) {
      if (s.value.trim()) {
        specification[s.key] = s.value.trim();
      }
    }
    return {
      name: this.name.trim(),
      localName: this.localName.trim() || null,
      url: this.url.trim(),
      categoryId: this.categoryId,
      subCategoryId: this.subCategoryId ?? null,
      brandId: this.brandId ?? null,
      variantType: this.variantType || null,
      status: this.status,
      inventoryStatus: this.inventoryStatus,
      inventoryCount: this.inventoryCount,
      weightGram: this.weightGram ?? null,
      shortDescription: this.shortDescription.trim() || null,
      fullDescription: this.fullDescription.trim() || null,
      prices,
      specification
    };
  }

  private fill(p: ProductDto): void {
    this.name = p.name ?? '';
    this.localName = p.localName ?? '';
    this.url = p.url ?? '';
    this.categoryId = p.categoryId ?? null;
    this.subCategoryId = p.subCategoryId ?? null;
    this.brandId = p.brandId ?? null;
    this.variantType = (p.variantType as VariantTypeValue) ?? '';
    this.status = (p.status as ProductStatus) ?? 'ACTIVE';
    this.inventoryStatus = (p.inventoryStatus as InventoryStatus) ?? 'IN_STOCK';
    this.inventoryCount = p.inventoryCount ?? 0;
    this.weightGram = p.weightGram ?? null;
    this.shortDescription = p.shortDescription ?? '';
    this.fullDescription = p.fullDescription ?? '';
    // Money is stored in Rial but admins edit in Toman — divide on load, multiply back on save.
    const rows: PriceRow[] = (p.prices ?? []).map((pr) => ({
      variantValue: pr.variantValue ?? '',
      price: pr.price != null ? Number(pr.price) / 10 : null,
      discountPrice: pr.discountPrice != null ? Number(pr.discountPrice) / 10 : null
    }));
    this.prices.set(rows.length ? rows : [{ variantValue: '', price: null, discountPrice: null }]);
    this.specs.set(
      Object.entries(p.specification ?? {}).map(([key, value]) => ({
        key: key as SpecificationKey,
        value: String(value)
      }))
    );
  }

  private refreshImages(): void {
    if (!this.isEdit) {
      return;
    }
    this.productApi.getById(this.editId()!).subscribe({ next: (p) => this.product.set(p) });
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2000);
  }
}
