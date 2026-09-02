import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';
import { warehouseGuard } from './core/warehouse.guard';

/**
 * Route table used ONLY on the dashboard host (dashboard.rivany.ir). The storefront routes are not
 * registered here, so the shop is unreachable on that domain. One shared login serves both staff
 * kinds; landing on `/` routes each signed-in user to their own area (admin panel or warehouse
 * console) by role, and everyone else to login.
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
      },
      {
        path: 'staff',
        loadComponent: () => import('./admin/staff/staff').then((m) => m.StaffAdmin)
      }
    ]
  },
  {
    path: 'warehouse',
    canMatch: [warehouseGuard],
    loadComponent: () => import('./warehouse/shell/warehouse-shell').then((m) => m.WarehouseShell),
    children: [
      {
        path: '',
        loadComponent: () => import('./warehouse/orders/order-list').then((m) => m.WarehouseOrderList)
      },
      {
        path: 'orders/:id',
        loadComponent: () =>
          import('./warehouse/orders/order-detail').then((m) => m.WarehouseOrderDetail)
      }
    ]
  },
  { path: '', pathMatch: 'full', loadComponent: () => import('./dashboard-home').then((m) => m.DashboardHome) },
  { path: '**', redirectTo: '' }
];
