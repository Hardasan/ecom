import { Component, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/auth.service';
import { CartService } from './core/cart.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);

  // The admin area renders full-bleed; the storefront stays in the centered 390px phone frame.
  readonly isAdminRoute = signal(this.router.url.startsWith('/admin'));

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => this.isAdminRoute.set(e.urlAfterRedirects.startsWith('/admin')));

    // Keep the cart badge in sync with auth state: load on app start / login, clear on logout.
    effect(() => {
      if (this.auth.isLoggedIn()) {
        this.cart.refresh();
      } else {
        this.cart.clear();
      }
    });
  }
}
