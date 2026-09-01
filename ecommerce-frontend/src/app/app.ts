import { Component, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/auth.service';
import { CartService } from './core/cart.service';
import { isDashboardHost } from './core/host';

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
  // On the dashboard host every route is admin, so it is always full-bleed.
  readonly isAdminRoute = signal(isDashboardHost() || this.router.url.startsWith('/admin'));

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) =>
        this.isAdminRoute.set(isDashboardHost() || e.urlAfterRedirects.startsWith('/admin'))
      );

    // Keep the cart in sync with auth state across transitions. A guest keeps a local cart, so we
    // must not wipe it on first load; we only drop it on an actual sign-out. The guest→signed-in
    // merge is owned by the login page (it awaits the merge before navigating), so here we just
    // (re)load the badge on initial load and on sign-out.
    let wasLoggedIn = this.auth.isLoggedIn();
    effect(() => {
      const loggedIn = this.auth.isLoggedIn();
      const transitioned = loggedIn !== wasLoggedIn;
      const previous = wasLoggedIn;
      wasLoggedIn = loggedIn;
      if (transitioned && !loggedIn) {
        this.cart.onLogout(); // just signed out — drop the guest cart and clear the badge
      } else if (transitioned && loggedIn) {
        // just signed in — login page merges the guest cart; nothing to do here
      } else if (!previous || loggedIn) {
        this.cart.refresh(); // initial load (guest or server cart), or a reload while signed in
      }
    });
  }
}
