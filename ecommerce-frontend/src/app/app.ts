import { Component, computed, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './core/auth.service';
import { CartService } from './core/cart.service';
import { LayoutService } from './core/layout.service';
import { isDashboardHost } from './core/host';
import { SiteHeader } from './shared/site-header/site-header';
import { SiteFooter } from './shared/site-footer/site-footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, SiteHeader, SiteFooter],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  readonly layout = inject(LayoutService);

  // The admin area renders full-bleed; the storefront stays in the centered 390px phone frame.
  // On the dashboard host every route is admin, so it is always full-bleed.
  readonly isAdminRoute = signal(isDashboardHost() || this.router.url.startsWith('/admin'));

  // The storefront (everything that is not admin/warehouse) gets the full-width desktop chrome —
  // global site header + footer, document scroll — but only at desktop width; phones keep the
  // per-page top-bars and bottom-nav inside the 390px frame.
  readonly showDesktopChrome = computed(() => !this.isAdminRoute() && this.layout.isDesktop());

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
