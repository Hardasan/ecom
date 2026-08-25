import { Routes } from '@angular/router';
import { adminGuard } from './core/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then((m) => m.Home)
  },
  {
    path: 'categories',
    loadComponent: () => import('./pages/categories/categories').then((m) => m.Categories)
  },
  {
    path: 'products',
    loadComponent: () => import('./pages/product-list/product-list').then((m) => m.ProductList)
  },
  {
    path: 'search',
    loadComponent: () => import('./pages/search/search').then((m) => m.SearchPage)
  },
  {
    path: 'product/:id',
    loadComponent: () => import('./pages/product/product').then((m) => m.Product)
  },
  { path: 'product', redirectTo: 'products' },
  {
    path: 'cart',
    loadComponent: () => import('./pages/cart/cart').then((m) => m.Cart)
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.Login)
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile').then((m) => m.Profile)
  },
  {
    path: 'orders',
    loadComponent: () => import('./pages/orders/orders').then((m) => m.Orders)
  },
  {
    path: 'orders/:orderId',
    loadComponent: () => import('./pages/order-detail/order-detail').then((m) => m.OrderDetail)
  },
  {
    path: 'checkout',
    loadComponent: () => import('./pages/checkout/checkout').then((m) => m.Checkout)
  },
  {
    path: 'success/:orderId',
    loadComponent: () => import('./pages/success/success').then((m) => m.Success)
  },
  {
    path: 'success',
    loadComponent: () => import('./pages/success/success').then((m) => m.Success)
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
  { path: '**', redirectTo: '' }
];
