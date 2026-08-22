import { Component, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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

  constructor() {
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
