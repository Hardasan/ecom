import { Component, OnInit, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AddressService } from '../../core/address.service';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { OrderService } from '../../core/order.service';
import { GeoService } from '../../core/geo.service';
import {
  AddressDto,
  CartItemDto,
  CheckoutQuoteDto,
  GeoCityDto,
  GeoProvinceDto,
  PaymentMethod
} from '../../core/models';
import { colorHex, formatPrice, imageSrc, toNumber, variantLabel } from '../../core/format';
import { validateAddressFields } from '../../core/address-form';

@Component({
  selector: 'app-checkout',
  imports: [FormsModule, RouterLink, FaNumPipe],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss'
})
export class Checkout implements OnInit {
  readonly a = ASSETS;
  readonly provinces = signal<GeoProvinceDto[]>([]);
  readonly cities = signal<GeoCityDto[]>([]);
  private readonly auth = inject(AuthService);
  private readonly addressesApi = inject(AddressService);
  private readonly geoApi = inject(GeoService);
  private readonly cartApi = inject(CartService);
  private readonly ordersApi = inject(OrderService);
  private readonly router = inject(Router);

  readonly addresses = signal<AddressDto[]>([]);
  readonly selectedAddressId = signal<number | null>(null);
  readonly cartItems = signal<CartItemDto[]>([]);
  readonly cartTotal = signal(0);
  readonly cartQty = signal(0);
  readonly quote = signal<CheckoutQuoteDto | null>(null);
  readonly showSelectSheet = signal(false);
  readonly showAddressSheet = signal(false);
  readonly showItemsSheet = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});

  readonly selectedAddress = computed(() => {
    const id = this.selectedAddressId();
    return this.addresses().find((a) => a.id === id) ?? null;
  });

  payment = 'bank';
  title = 'خانه';
  recipientFirstName = '';
  recipientLastName = '';
  recipientMobile = '';
  province: string = 'TEHRAN';
  city = 'تهران';
  addressLine = '';
  postalCode = '';
  plaque = '';

  constructor() {
    // Whenever the chosen address (or the cart) changes, re-quote so the shown total — and the
    // amount actually charged — reflects this address's shipping cost.
    effect(() => {
      const addressId = this.selectedAddressId();
      const hasItems = this.cartItems().length > 0;
      if (addressId != null && hasItems) {
        this.loadQuote(addressId);
      } else {
        this.quote.set(null);
      }
    });
  }

  private loadQuote(addressId: number) {
    this.ordersApi.quote(addressId).subscribe({
      next: (q) => this.quote.set(q),
      error: () => this.quote.set(null)
    });
  }

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/checkout' } });
      return;
    }
    this.reloadAddresses();
    this.geoApi.listProvinces().subscribe({
      next: (res) => this.provinces.set(res.provinces ?? []),
      error: () => this.error.set('لیست استان‌ها خوانده نشد')
    });
    this.loadCities('تهران');
    this.cartApi.get().subscribe({
      next: (c) => {
        this.cartItems.set(c.items ?? []);
        this.cartTotal.set(toNumber(c.totalPrice));
        this.cartQty.set(c.totalQuantity ?? 0);
      },
      error: () => this.error.set('سبد خرید خوانده نشد')
    });
  }

  reloadAddresses() {
    this.addressesApi.list().subscribe({
      next: (list) => {
        this.addresses.set(list ?? []);
        const current = this.selectedAddressId();
        const still = list.find((a) => a.id === current);
        const def = still ?? list.find((a) => a.isDefault) ?? list[0];
        this.selectedAddressId.set(def?.id ?? null);
      },
      error: () => this.error.set('آدرس‌ها خوانده نشد')
    });
  }

  openSelectSheet() {
    this.showAddressSheet.set(false);
    this.showItemsSheet.set(false);
    this.showSelectSheet.set(true);
  }

  closeSelectSheet() {
    this.showSelectSheet.set(false);
  }

  openFormSheet() {
    this.fieldErrors.set({});
    this.showSelectSheet.set(false);
    this.showItemsSheet.set(false);
    this.showAddressSheet.set(true);
  }

  clearFieldError(field: string) {
    const errors = this.fieldErrors();
    if (errors[field]) {
      const { [field]: _removed, ...rest } = errors;
      this.fieldErrors.set(rest);
    }
  }

  closeFormSheet() {
    this.showAddressSheet.set(false);
  }

  onProvinceChange() {
    this.loadCities();
  }

  openItemsSheet() {
    this.showSelectSheet.set(false);
    this.showAddressSheet.set(false);
    this.showItemsSheet.set(true);
  }

  closeItemsSheet() {
    this.showItemsSheet.set(false);
  }

  selectAddress(id: number | undefined) {
    if (id != null) {
      this.selectedAddressId.set(id);
      this.closeSelectSheet();
    }
  }

  addressText(addr: AddressDto): string {
    const parts = [addr.city, addr.addressLine];
    if (addr.plaque) {
      parts.push(`پلاک ${addr.plaque}`);
    }
    if (addr.unit) {
      parts.push(`واحد ${addr.unit}`);
    }
    return parts.filter(Boolean).join('، ');
  }

  itemThumb(item: CartItemDto): string {
    return imageSrc(item.mainImage);
  }

  totalLabel(): string {
    return formatPrice(this.cartTotal());
  }

  /** Items subtotal (from the quote once an address is chosen, else the cart total). */
  itemsSubtotalLabel(): string {
    const q = this.quote();
    return formatPrice(q ? q.itemsCost : this.cartTotal());
  }

  /** True once we know shipping (an address is selected and the quote has loaded). */
  hasShipping(): boolean {
    return this.quote() != null;
  }

  shippingLabel(): string {
    const q = this.quote();
    return q ? formatPrice(q.shippingCost) : 'پس از انتخاب آدرس';
  }

  /** The amount actually charged: items + shipping once quoted, else the items subtotal. */
  payableLabel(): string {
    const q = this.quote();
    return formatPrice(q ? q.totalCost : this.cartTotal());
  }

  payButtonLabel(): string {
    if (this.busy()) {
      return '…';
    }
    if (this.selectedAddressId() == null) {
      return 'تعیین آدرس';
    }
    const verb = this.payment === 'cod' ? 'ثبت سفارش' : 'پرداخت';
    return `${verb} ${this.payableLabel()}`;
  }

  variantHex(item: CartItemDto): string {
    return colorHex(item.variantValue);
  }

  variantText(item: CartItemDto): string {
    return variantLabel(item.variantType, item.variantValue);
  }

  saveAddress() {
    const errors = validateAddressFields({
      recipientFirstName: this.recipientFirstName,
      recipientLastName: this.recipientLastName,
      recipientMobile: this.recipientMobile,
      city: this.city,
      postalCode: this.postalCode,
      addressLine: this.addressLine
    });
    this.fieldErrors.set(errors);
    if (Object.keys(errors).length) {
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.addressesApi
      .create({
        title: this.title || 'خانه',
        recipientFirstName: this.recipientFirstName.trim(),
        recipientLastName: this.recipientLastName.trim(),
        recipientMobile: this.recipientMobile.replace(/\D/g, ''),
        province: this.province,
        city: this.city.trim(),
        postalCode: this.postalCode.trim(),
        addressLine: this.addressLine.trim(),
        plaque: this.plaque || undefined,
        isDefault: this.addresses().length === 0
      })
      .subscribe({
        next: (created) => {
          this.busy.set(false);
          this.closeFormSheet();
          this.reloadAddresses();
          if (created.id != null) {
            this.selectedAddressId.set(created.id);
          }
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err?.error?.message ?? 'ثبت آدرس ناموفق بود');
        }
      });
  }

  placeOrder() {
    const addressId = this.selectedAddressId();
    if (addressId == null) {
      if (this.addresses().length) {
        this.openSelectSheet();
      } else {
        this.openFormSheet();
      }
      this.error.set('ابتدا یک آدرس انتخاب یا ثبت کنید');
      return;
    }
    if (!this.cartItems().length) {
      this.error.set('سبد خرید خالی است');
      return;
    }
    this.busy.set(true);
    this.error.set('');

    const method: PaymentMethod = this.payment === 'cod' ? 'CASH_ON_DELIVERY' : 'ONLINE';
    this.ordersApi.checkout(addressId, method).subscribe({
      next: (order) => {
        if (method === 'CASH_ON_DELIVERY') {
          // Cash on delivery: the order is placed; no online payment step.
          this.busy.set(false);
          this.cartApi.clear();
          void this.router.navigate(['/success', order.id]);
          return;
        }
        this.ordersApi.pay(order.id).subscribe({
          next: (pay) => {
            this.ordersApi.confirmPayment(order.id, pay.paymentReference).subscribe({
              next: () => {
                this.busy.set(false);
                this.cartApi.clear();
                void this.router.navigate(['/success', order.id]);
              },
              error: (err) => this.fail(err)
            });
          },
          error: (err) => this.fail(err)
        });
      },
      error: (err) => this.fail(err)
    });
  }

  lineLabel(item: CartItemDto): string {
    return item.productName;
  }

  lineMeta(item: CartItemDto): string {
    return formatPrice(item.lineTotal);
  }

  private loadCities(preferredCity?: string) {
    this.geoApi.listCities(this.province).subscribe({
      next: (res) => {
        const list = res.cities ?? [];
        this.cities.set(list);
        const names = list.map((c) => c.name);
        this.city = preferredCity && names.includes(preferredCity) ? preferredCity : (names[0] ?? '');
      },
      error: () => this.error.set('لیست شهرها خوانده نشد')
    });
  }

  private fail(err: { error?: { message?: string } }) {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'ثبت سفارش ناموفق بود');
  }
}
