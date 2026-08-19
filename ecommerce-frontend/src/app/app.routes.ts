import { Routes } from '@angular/router';

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
  { path: '**', redirectTo: '' }
];
