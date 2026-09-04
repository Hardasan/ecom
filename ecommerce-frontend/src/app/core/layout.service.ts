import { Injectable, computed, signal } from '@angular/core';

/**
 * The single source of truth for the mobile ⇄ desktop breakpoint.
 *
 * The storefront ships two form factors from one build: the phone/PWA UI (the centered
 * 390px frame with bottom-nav) and a full-width desktop UI (site header + footer, multi-column
 * layouts). This service exposes that split as a signal so templates can branch the two UIs
 * directly — e.g. `@if (layout.isDesktop()) { …desktop… } @else { …mobile… }` — which is the
 * seam we keep clean so the PWA and desktop UIs can diverge further later without a rewrite.
 *
 * Keep {@link DESKTOP_MIN_WIDTH} in sync with `$bp-desktop` in `styles.scss`; the CSS media
 * queries and this signal must flip at the same width or chrome and content disagree.
 */
export const DESKTOP_MIN_WIDTH = 1024;

@Injectable({ providedIn: 'root' })
export class LayoutService {
  // Guard `window` so the service is safe under non-browser rendering (tests / future SSR).
  private readonly query =
    typeof window !== 'undefined' && typeof window.matchMedia === 'function'
      ? window.matchMedia(`(min-width: ${DESKTOP_MIN_WIDTH}px)`)
      : null;

  /** True on desktop-width viewports (≥ 1024px). Drives desktop chrome and layouts. */
  readonly isDesktop = signal(this.query?.matches ?? false);

  /** Convenience inverse of {@link isDesktop} for readable templates. */
  readonly isMobile = computed(() => !this.isDesktop());

  constructor() {
    // `matchMedia` fires only on breakpoint crossings, so this is cheap — one listener, no polling.
    this.query?.addEventListener('change', (e) => this.isDesktop.set(e.matches));
  }
}
