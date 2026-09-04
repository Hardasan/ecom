import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ASSETS } from '../../assets';
import { AddressService } from '../../core/address.service';
import { AuthService } from '../../core/auth.service';
import { GeoService } from '../../core/geo.service';
import { OrderService } from '../../core/order.service';
import { AddressDto, GeoCityDto, GeoProvinceDto, OrderDto } from '../../core/models';
import { formatFaDate, formatPrice, imageSrc, orderItemCount, orderStatusLabel } from '../../core/format';
import { validateAddressFields } from '../../core/address-form';
import { BottomNav } from '../../shared/bottom-nav/bottom-nav';

@Component({
  selector: 'app-profile',
  imports: [FormsModule, RouterLink, FaNumPipe, BottomNav],
  templateUrl: './profile.html',
  styleUrl: './profile.scss'
})
export class Profile implements OnInit {
  readonly a = ASSETS;
  readonly provinces = signal<GeoProvinceDto[]>([]);
  readonly cities = signal<GeoCityDto[]>([]);
  private readonly auth = inject(AuthService);
  private readonly addressesApi = inject(AddressService);
  private readonly geoApi = inject(GeoService);
  private readonly ordersApi = inject(OrderService);
  private readonly router = inject(Router);

  readonly addresses = signal<AddressDto[]>([]);
  readonly recentOrders = signal<OrderDto[]>([]);
  readonly ordersCount = signal(0);
  readonly showAddressSheet = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});

  // The menu-hub keeps the account-edit form and the address book collapsed until their row/stat
  // is tapped, so the landing view stays the clean list of destinations from the design.
  readonly showAccount = signal(false);
  readonly showAddresses = signal(false);

  firstName = '';
  lastName = '';
  mobile = '';

  editingAddressId: number | null = null;
  title = 'خانه';
  recipientFirstName = '';
  recipientLastName = '';
  recipientMobile = '';
  province: string = 'TEHRAN';
  city = 'تهران';
  addressLine = '';
  postalCode = '';
  plaque = '';

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/profile' } });
      return;
    }
    this.auth.getProfile().subscribe({
      next: (p) => {
        this.firstName = p.firstName ?? '';
        this.lastName = p.lastName ?? '';
        this.mobile = p.mobile ?? '';
      },
      error: () => this.error.set('اطلاعات حساب خوانده نشد')
    });
    this.reloadAddresses();
    this.reloadRecentOrders();
    this.geoApi.listProvinces().subscribe({
      next: (res) => this.provinces.set(res.provinces ?? []),
      error: () => this.error.set('لیست استان‌ها خوانده نشد')
    });
    this.loadCities('تهران');
  }

  dateLabel(order: OrderDto): string {
    return formatFaDate(order.createdAt);
  }

  tracking(order: OrderDto): string {
    return `کد پیگیری: ${order.id}`;
  }

  total(order: OrderDto): string {
    return formatPrice(order.totalCost);
  }

  qty(order: OrderDto): string {
    return `تعداد کل کالاها: ${orderItemCount(order.items)}`;
  }

  status(order: OrderDto): string {
    return orderStatusLabel(order.status, order.paymentMethod);
  }

  thumbs(order: OrderDto): string[] {
    return (order.items ?? [])
      .map((item) => imageSrc(item.mainImage))
      .filter((src) => !!src)
      .slice(0, 3);
  }

  saveProfile() {
    const mobile = this.mobile.replace(/\D/g, '');
    if (!this.firstName.trim() || !this.lastName.trim() || !/^09\d{9}$/.test(mobile)) {
      this.error.set('نام، نام خانوادگی و شماره موبایل معتبر لازم است');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.auth
      .updateProfile({
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        mobile
      })
      .subscribe({
        next: (p) => {
          this.mobile = p.mobile;
          this.busy.set(false);
          this.flash('اطلاعات حساب ذخیره شد');
        },
        error: (err) => this.fail(err)
      });
  }

  openNewAddress() {
    this.fieldErrors.set({});
    this.editingAddressId = null;
    this.title = 'خانه';
    this.recipientFirstName = this.firstName;
    this.recipientLastName = this.lastName;
    this.recipientMobile = this.mobile;
    this.province = 'TEHRAN';
    this.loadCities('تهران');
    this.addressLine = '';
    this.postalCode = '';
    this.plaque = '';
    this.showAddressSheet.set(true);
  }

  openEditAddress(addr: AddressDto) {
    this.fieldErrors.set({});
    this.editingAddressId = addr.id ?? null;
    this.title = addr.title || 'خانه';
    this.recipientFirstName = addr.recipientFirstName;
    this.recipientLastName = addr.recipientLastName;
    this.recipientMobile = addr.recipientMobile;
    this.province = addr.province;
    this.loadCities(addr.city);
    this.addressLine = addr.addressLine;
    this.postalCode = addr.postalCode;
    this.plaque = addr.plaque ?? '';
    this.showAddressSheet.set(true);
  }

  closeSheet() {
    this.showAddressSheet.set(false);
  }

  onProvinceChange() {
    this.loadCities();
  }

  clearFieldError(field: string) {
    const errors = this.fieldErrors();
    if (errors[field]) {
      const { [field]: _removed, ...rest } = errors;
      this.fieldErrors.set(rest);
    }
  }

  private validateAddress(): boolean {
    const errors = validateAddressFields({
      recipientFirstName: this.recipientFirstName,
      recipientLastName: this.recipientLastName,
      recipientMobile: this.recipientMobile,
      city: this.city,
      postalCode: this.postalCode,
      addressLine: this.addressLine
    });
    this.fieldErrors.set(errors);
    return Object.keys(errors).length === 0;
  }

  saveAddress() {
    if (!this.validateAddress()) {
      return;
    }
    const body: AddressDto = {
      title: this.title || 'خانه',
      recipientFirstName: this.recipientFirstName.trim(),
      recipientLastName: this.recipientLastName.trim(),
      recipientMobile: this.recipientMobile.replace(/\D/g, ''),
      province: this.province,
      city: this.city.trim(),
      postalCode: this.postalCode.trim(),
      addressLine: this.addressLine.trim(),
      plaque: this.plaque || undefined
    };
    this.busy.set(true);
    this.error.set('');
    const req$ =
      this.editingAddressId != null
        ? this.addressesApi.update(this.editingAddressId, body)
        : this.addressesApi.create({ ...body, isDefault: this.addresses().length === 0 });
    req$.subscribe({
      next: () => {
        this.busy.set(false);
        this.closeSheet();
        this.reloadAddresses();
        this.flash('آدرس ذخیره شد');
      },
      error: (err) => this.fail(err)
    });
  }

  deleteAddress(addr: AddressDto, event: Event) {
    event.stopPropagation();
    if (addr.id == null) {
      return;
    }
    this.addressesApi.delete(addr.id).subscribe({
      next: () => this.reloadAddresses(),
      error: (err) => this.fail(err)
    });
  }

  logout() {
    this.auth.logout();
    void this.router.navigateByUrl('/');
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

  private reloadAddresses() {
    this.addressesApi.list().subscribe({
      next: (list) => this.addresses.set(list ?? []),
      error: () => this.error.set('آدرس‌ها خوانده نشد')
    });
  }

  private reloadRecentOrders() {
    this.ordersApi.list().subscribe({
      next: (list) => {
        this.recentOrders.set((list ?? []).slice(0, 2));
        this.ordersCount.set((list ?? []).length);
      },
      error: () => {
        /* profile still usable without orders */
      }
    });
  }

  /** Shopper display name for the hub header; falls back to the mobile number. */
  displayName(): string {
    const name = `${this.firstName} ${this.lastName}`.trim();
    return name || this.mobile || 'کاربر ریونی';
  }

  toggleAccount() {
    this.showAccount.update((v) => !v);
  }

  showAddressBook() {
    this.showAddresses.set(true);
    this.showAccount.set(false);
  }

  private flash(message: string) {
    this.toast.set(message);
    setTimeout(() => this.toast.set(''), 2000);
  }

  private fail(err: { error?: { message?: string } }) {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'عملیات ناموفق بود');
  }
}
