import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';

export type BottomNavKey = 'home' | 'cart' | 'categories' | 'profile';

/**
 * The phone/PWA bottom tab-bar (13-Shahrivar design): خانه · سبد خرید · دسته‌بندی · پروفایل.
 * Shared so every storefront page renders the identical nav instead of copy-pasting the markup;
 * it is hidden on desktop by the global `.app-shell--desktop nav.bottom-nav { display:none }` rule.
 *
 * Icons are recoloured with a CSS mask (`background: currentColor`) so the active tab tints both
 * its icon and label brand-green — the previous per-page nav only coloured the label.
 */
@Component({
  selector: 'app-bottom-nav',
  imports: [RouterLink],
  templateUrl: './bottom-nav.html',
  styleUrl: './bottom-nav.scss'
})
export class BottomNav {
  /** Which tab is highlighted. Pages pass their own key, e.g. `active="home"`. */
  readonly active = input<BottomNavKey | null>(null);

  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
}
