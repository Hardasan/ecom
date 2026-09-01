import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withViewTransitions } from '@angular/router';

import { routes } from './app.routes';
import { adminRoutes } from './admin.routes';
import { authInterceptor } from './core/auth.interceptor';
import { isDashboardHost } from './core/host';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // The admin dashboard host (dashboard.rivany.ir) gets an admin-only route table; every other
    // host gets the storefront. Same build, chosen once at bootstrap from the hostname.
    provideRouter(isDashboardHost() ? adminRoutes : routes, withViewTransitions()),
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
