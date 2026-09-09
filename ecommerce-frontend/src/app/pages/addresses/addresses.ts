import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';
import { AddressService } from '../../core/address.service';
import { AuthService } from '../../core/auth.service';
import { GeoService } from '../../core/geo.service';
import { AddressDto, GeoCityDto, GeoProvinceDto } from '../../core/models';
import { validateAddressFields } from '../../core/address-form';

/**
 * Address book — a dedicated page (reached from the profile menu «آدرس‌ها») with a back button,
 * instead of an inline section. Lists the user's addresses and manages create/edit/delete through a
 * bottom-sheet form. The address logic mirrors what the checkout sheet uses.
 */
@Component({
  selector: 'app-addresses',
  imports: [FormsModule, RouterLink],
  templateUrl: './addresses.html',
  styleUrl: './addresses.scss'
})
export class AddressesPage implements OnInit {
  readonly a = ASSETS;
  private readonly auth = inject(AuthService);
  private readonly addressesApi = inject(AddressService);
  private readonly geoApi = inject(GeoService);
  private readonly router = inject(Router);

  readonly provinces = signal<GeoProvinceDto[]>([]);
  readonly cities = signal<GeoCityDto[]>([]);
  readonly addresses = signal<AddressDto[]>([]);
  readonly loading = signal(true);
  readonly showAddressSheet = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly fieldErrors = signal<Record<string, string>>({});

  // The signed-in user's own name/mobile, used to prefill a *new* address's recipient fields.
  private firstName = '';
  private lastName = '';
  private mobile = '';

  editingAddressId: number | null = null;
  title = 'خانه';
  recipientFirstName = '';
  recipientLastName = '';
  recipientMobile = '';
  province = 'TEHRAN';
  city = 'تهران';
  addressLine = '';
  postalCode = '';
  plaque = '';

  ngOnInit(): void {
    if (!this.auth.isLoggedIn()) {
      void this.router.navigate(['/login'], { queryParams: { returnUrl: '/addresses' } });
      return;
    }
    this.auth.getProfile().subscribe({
      next: (p) => {
        this.firstName = p.firstName ?? '';
        this.lastName = p.lastName ?? '';
        this.mobile = p.mobile ?? '';
      },
      error: () => undefined
    });
    this.reloadAddresses();
    this.geoApi.listProvinces().subscribe({
      next: (res) => this.provinces.set(res.provinces ?? []),
      error: () => this.error.set('لیست استان‌ها خوانده نشد')
    });
    this.loadCities('تهران');
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
      next: (list) => {
        this.addresses.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('آدرس‌ها خوانده نشد');
        this.loading.set(false);
      }
    });
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
