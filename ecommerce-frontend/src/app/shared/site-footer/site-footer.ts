import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Desktop-only footer for the storefront. Rendered by {@link App} only when `layout.isDesktop()`
 * on a storefront route (the phone UI has no footer — it ends at the bottom-nav).
 */
@Component({
  selector: 'app-site-footer',
  imports: [RouterLink],
  templateUrl: './site-footer.html',
  styleUrl: './site-footer.scss'
})
export class SiteFooter {
  // Persian digits to match the rest of the shopper-facing UI (see the faNum pipe convention).
  readonly year = new Intl.NumberFormat('fa-IR', { useGrouping: false }).format(
    new Date().getFullYear()
  );
}
