import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AdminDiscountService } from '../services/admin-discount.service';
import { AdminCategoryService } from '../services/admin-category.service';
import { CategoryHierarchyItem, DiscountDto, DiscountScope, DiscountType } from '../../core/models';

@Component({
  selector: 'app-admin-discount-form',
  imports: [FormsModule, RouterLink],
  templateUrl: './discount-form.html',
  styleUrl: './discount-form.scss'
})
export class DiscountForm implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AdminDiscountService);
  private readonly categoryApi = inject(AdminCategoryService);

  readonly editId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly categories = signal<CategoryHierarchyItem[]>([]);
  readonly selectedCategoryIds = signal<Set<number>>(new Set());

  readonly types: DiscountType[] = ['PERCENTAGE', 'FIXED_AMOUNT'];
  readonly scopes: DiscountScope[] = ['ALL', 'PRODUCTS', 'CATEGORIES'];

  code = '';
  type: DiscountType = 'PERCENTAGE';
  value: number | null = null;
  maxDiscountAmount: number | null = null;
  minimumCartAmount: number | null = null;
  scope: DiscountScope = 'ALL';
  productIdsText = '';
  expiresAt = '';
  usageLimit: number | null = null;
  perUserLimit: number | null = null;

  ngOnInit(): void {
    this.categoryApi.hierarchy().subscribe({
      next: (r) => this.categories.set(r.categories ?? []),
      error: () => undefined
    });
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId.set(Number(id));
      this.load(Number(id));
    }
  }

  get isEdit(): boolean {
    return this.editId() !== null;
  }

  isCatSelected(id: number): boolean {
    return this.selectedCategoryIds().has(id);
  }

  toggleCategory(id: number): void {
    this.selectedCategoryIds.update((s) => {
      const next = new Set(s);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  allCategoryNodes(): { id: number; label: string }[] {
    const nodes: { id: number; label: string }[] = [];
    for (const item of this.categories()) {
      nodes.push({ id: item.category.id, label: item.category.localName || item.category.name });
      for (const sub of item.subCategories) {
        nodes.push({ id: sub.id, label: '— ' + (sub.localName || sub.name) });
      }
    }
    return nodes;
  }

  load(id: number): void {
    this.loading.set(true);
    this.api.get(id).subscribe({
      next: (d) => {
        this.fill(d);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('کد تخفیف یافت نشد');
        this.loading.set(false);
      }
    });
  }

  save(): void {
    if (!this.code.trim() || this.value == null) {
      this.error.set('کد و مقدار الزامی است');
      return;
    }
    const body: DiscountDto = {
      code: this.code.trim(),
      type: this.type,
      value: this.value,
      maxDiscountAmount: this.type === 'PERCENTAGE' ? this.maxDiscountAmount ?? null : null,
      minimumCartAmount: this.minimumCartAmount ?? null,
      scope: this.scope,
      productIds: this.scope === 'PRODUCTS' ? this.parseIds(this.productIdsText) : null,
      categoryIds: this.scope === 'CATEGORIES' ? Array.from(this.selectedCategoryIds()) : null,
      expiresAt: this.expiresAt ? new Date(this.expiresAt).toISOString() : null,
      usageLimit: this.usageLimit ?? null,
      perUserLimit: this.perUserLimit ?? null
    };
    this.busy.set(true);
    this.error.set('');
    const req = this.isEdit ? this.api.update(this.editId()!, body) : this.api.create(body);
    req.subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigate(['/admin/discounts']);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'ذخیره ناموفق بود');
      }
    });
  }

  private fill(d: DiscountDto): void {
    this.code = d.code;
    this.type = d.type;
    this.value = d.value != null ? Number(d.value) : null;
    this.maxDiscountAmount = d.maxDiscountAmount != null ? Number(d.maxDiscountAmount) : null;
    this.minimumCartAmount = d.minimumCartAmount != null ? Number(d.minimumCartAmount) : null;
    this.scope = d.scope;
    this.productIdsText = (d.productIds ?? []).join(', ');
    this.selectedCategoryIds.set(new Set(d.categoryIds ?? []));
    this.expiresAt = d.expiresAt ? new Date(d.expiresAt).toISOString().slice(0, 10) : '';
    this.usageLimit = d.usageLimit ?? null;
    this.perUserLimit = d.perUserLimit ?? null;
  }

  private parseIds(text: string): number[] {
    return text
      .split(/[,\s]+/)
      .map((t) => Number(t.trim()))
      .filter((n) => Number.isFinite(n) && n > 0);
  }
}
