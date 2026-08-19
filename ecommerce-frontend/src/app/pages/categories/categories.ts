import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CategoryService } from '../../core/category.service';
import { CategoryDto, CategoryHierarchyItem } from '../../core/models';

@Component({
  selector: 'app-categories',
  imports: [RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.scss'
})
export class Categories implements OnInit {
  readonly a = ASSETS;
  readonly auth = inject(AuthService);
  private readonly categoriesApi = inject(CategoryService);

  readonly items = signal<CategoryHierarchyItem[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  ngOnInit(): void {
    this.categoriesApi.hierarchy().subscribe({
      next: (res) => {
        this.items.set(res.categories ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('دسته‌بندی‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  nameOf(c: CategoryDto): string {
    return c.localName || c.name;
  }
}
