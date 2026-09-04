import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { ASSETS } from '../../assets';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { CategoryService } from '../../core/category.service';
import { CategoryDto } from '../../core/models';

/**
 * Desktop-only top chrome for the storefront (the phone UI uses per-page top-bars + bottom-nav
 * instead). Rendered by {@link App} only when `layout.isDesktop()` on a storefront route, so it is
 * never in the phone DOM. Mirrors the mobile bottom-nav destinations (home / categories / search /
 * account) plus a cart button, so navigation parity holds across form factors.
 */
@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive, FormsModule],
  templateUrl: './site-header.html',
  styleUrl: './site-header.scss'
})
export class SiteHeader implements OnInit {
  readonly a = ASSETS;
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  private readonly categoriesApi = inject(CategoryService);
  private readonly router = inject(Router);

  readonly rootCategories = signal<CategoryDto[]>([]);
  query = '';

  ngOnInit(): void {
    this.categoriesApi.list().subscribe({
      next: (res) => this.rootCategories.set((res.categories ?? []).filter((c) => !c.parentId)),
      error: () => undefined
    });
  }

  search(): void {
    const q = this.query.trim();
    if (!q) return;
    void this.router.navigate(['/products'], { queryParams: { q, title: `جستجو: ${q}` } });
  }
}
