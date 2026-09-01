import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';

/**
 * Route table used ONLY on the admin dashboard host (dashboard.rivany.ir). The storefront routes are
 * not registered here, so the shop is unreachable on that domain. Landing on `/` sends the visitor to
 * the guarded dashboard, which bounces to the admin login when they are not signed in as an admin.
 */
export const adminRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./admin/login/admin-login').then((m) => m.AdminLogin)
  },
  {
    path: 'admin',
    canMatch: [adminGuard],
    loadComponent: () => import('./admin/shell/admin-shell').then((m) => m.AdminShell),
    children: [
      {
        path: '',
        loadComponent: () => import('./admin/dashboard/dashboard').then((m) => m.Dashboard)
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./admin/products/product-list').then((m) => m.ProductListAdmin)
      },
      {
        path: 'products/new',
        loadComponent: () => import('./admin/products/product-form').then((m) => m.ProductForm)
      },
      {
        path: 'products/:id',
        loadComponent: () => import('./admin/products/product-form').then((m) => m.ProductForm)
      },
      {
        path: 'categories',
        loadComponent: () => import('./admin/categories/categories').then((m) => m.CategoriesAdmin)
      },
      {
        path: 'discounts',
        loadComponent: () =>
          import('./admin/discounts/discount-list').then((m) => m.DiscountListAdmin)
      },
      {
        path: 'discounts/new',
        loadComponent: () => import('./admin/discounts/discount-form').then((m) => m.DiscountForm)
      },
      {
        path: 'discounts/:id',
        loadComponent: () => import('./admin/discounts/discount-form').then((m) => m.DiscountForm)
      },
      {
        path: 'orders',
        loadComponent: () => import('./admin/orders/order-list').then((m) => m.OrderListAdmin)
      },
      {
        path: 'orders/:id',
        loadComponent: () => import('./admin/orders/order-detail').then((m) => m.OrderDetailAdmin)
      },
      {
        path: 'reviews',
        loadComponent: () => import('./admin/reviews/reviews').then((m) => m.ReviewsAdmin)
      }
    ]
  },
  { path: '', pathMatch: 'full', redirectTo: 'admin' },
  { path: '**', redirectTo: 'admin' }
];
