import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminCategoryService } from '../services/admin-category.service';
import { CategoryDto, CategoryHierarchyItem } from '../../core/models';

@Component({
  selector: 'app-admin-categories',
  imports: [FormsModule],
  templateUrl: './categories.html',
  styleUrl: './categories.scss'
})
export class CategoriesAdmin implements OnInit {
  private readonly api = inject(AdminCategoryService);

  readonly items = signal<CategoryHierarchyItem[]>([]);
  readonly subDrafts = signal<Record<number, { name: string; localName: string }>>({});
  readonly loading = signal(true);
  readonly error = signal('');
  readonly toast = signal('');

  newRootName = '';
  newRootLocal = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.api.hierarchy().subscribe({
      next: (r) => {
        const items = r.categories ?? [];
        this.items.set(items);
        const drafts: Record<number, { name: string; localName: string }> = {};
        for (const it of items) {
          drafts[it.category.id] = { name: '', localName: '' };
        }
        this.subDrafts.set(drafts);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('دسته‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  addRoot(): void {
    if (!this.newRootName.trim()) {
      return;
    }
    this.api
      .create({ name: this.newRootName.trim(), localName: this.newRootLocal.trim() || undefined })
      .subscribe({
        next: () => {
          this.newRootName = '';
          this.newRootLocal = '';
          this.flash('دسته اضافه شد');
          this.load();
        },
        error: (e) => this.fail(e)
      });
  }

  addSub(parentId: number): void {
    const draft = this.subDrafts()[parentId];
    if (!draft?.name.trim()) {
      return;
    }
    this.api
      .createSub(parentId, { name: draft.name.trim(), localName: draft.localName.trim() || undefined })
      .subscribe({
        next: () => {
          this.flash('زیردسته اضافه شد');
          this.load();
        },
        error: (e) => this.fail(e)
      });
  }

  rename(cat: CategoryDto): void {
    const localName = prompt('نام فارسی دسته:', cat.localName ?? '');
    if (localName == null) {
      return;
    }
    this.api
      .update(cat.id, {
        name: cat.name,
        localName: localName.trim() || undefined,
        parentId: cat.parentId ?? undefined
      })
      .subscribe({
        next: () => {
          this.flash('ذخیره شد');
          this.load();
        },
        error: (e) => this.fail(e)
      });
  }

  remove(id: number, name: string): void {
    if (!confirm(`حذف «${name}»؟`)) {
      return;
    }
    this.api.delete(id).subscribe({
      next: () => {
        this.flash('حذف شد');
        this.load();
      },
      error: (e) => this.fail(e)
    });
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2000);
  }

  private fail(e: { error?: { message?: string } }): void {
    this.error.set(e?.error?.message ?? 'عملیات ناموفق بود');
  }
}
