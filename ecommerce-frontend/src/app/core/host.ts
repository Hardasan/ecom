/**
 * Host-based mode switch. The same Angular build is served on both the storefront domain
 * (e.g. rivany.ir) and the admin domain (dashboard.rivany.ir); the app decides which experience to
 * show from the browser's hostname, so no separate build or backend change is needed.
 */
export function isDashboardHost(): boolean {
  if (typeof window === 'undefined') {
    return false;
  }
  const host = window.location.hostname;
  return host === 'dashboard' || host.startsWith('dashboard.');
}

/**
 * Storefront origin derived from the current (dashboard) host — e.g. dashboard.rivany.ir → rivany.ir,
 * dashboard.localhost:4200 → localhost:4200. Used for "back to shop" links that must cross domains.
 */
export function storefrontUrl(): string {
  if (typeof window === 'undefined') {
    return '/';
  }
  const { protocol, host } = window.location;
  return `${protocol}//${host.replace(/^dashboard\./, '')}`;
}
