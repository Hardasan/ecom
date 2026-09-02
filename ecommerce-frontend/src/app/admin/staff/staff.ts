import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminStaffService } from '../services/admin-staff.service';
import { StaffDto } from '../../core/models';
import { formatFaDate } from '../../core/format';

@Component({
  selector: 'app-admin-staff',
  imports: [FormsModule],
  templateUrl: './staff.html',
  styleUrl: './staff.scss'
})
export class StaffAdmin implements OnInit {
  private readonly api = inject(AdminStaffService);

  readonly staff = signal<StaffDto[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly toast = signal('');
  readonly showCreate = signal(false);

  readonly date = formatFaDate;

  firstName = '';
  lastName = '';
  mobile = '';
  password = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (l) => {
        this.staff.set(l ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('فهرست کارکنان خوانده نشد');
        this.loading.set(false);
      }
    });
  }

  openCreate(): void {
    this.firstName = '';
    this.lastName = '';
    this.mobile = '';
    this.password = '';
    this.error.set('');
    this.showCreate.set(true);
  }

  create(): void {
    const mobile = this.mobile.replace(/\D/g, '');
    if (!this.firstName.trim() || !this.lastName.trim()) {
      this.error.set('نام و نام خانوادگی لازم است');
      return;
    }
    if (!/^09\d{9}$/.test(mobile)) {
      this.error.set('شماره موبایل معتبر نیست');
      return;
    }
    if (this.password.length < 6) {
      this.error.set('رمز عبور باید حداقل ۶ نویسه باشد');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.api
      .create({ firstName: this.firstName.trim(), lastName: this.lastName.trim(), mobile, password: this.password })
      .subscribe({
        next: (created) => {
          this.staff.update((list) => [created, ...list]);
          this.busy.set(false);
          this.showCreate.set(false);
          this.flash('کارمند جدید ایجاد شد');
        },
        error: (e) => {
          this.busy.set(false);
          this.error.set(e?.error?.message ?? 'ایجاد کارمند ناموفق بود');
        }
      });
  }

  toggle(member: StaffDto): void {
    this.busy.set(true);
    this.api.setStatus(member.id, !member.enabled).subscribe({
      next: (updated) => {
        this.staff.update((list) => list.map((m) => (m.id === updated.id ? updated : m)));
        this.busy.set(false);
        this.flash(updated.enabled ? 'حساب فعال شد' : 'حساب غیرفعال شد');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'تغییر وضعیت ناموفق بود');
      }
    });
  }

  resetPassword(member: StaffDto): void {
    const next = prompt(`رمز عبور جدید برای ${member.firstName} ${member.lastName} (حداقل ۶ نویسه):`);
    if (next == null) {
      return;
    }
    if (next.length < 6) {
      this.error.set('رمز عبور باید حداقل ۶ نویسه باشد');
      return;
    }
    this.busy.set(true);
    this.api.resetPassword(member.id, next).subscribe({
      next: () => {
        this.busy.set(false);
        this.flash('رمز عبور تغییر کرد');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.error?.message ?? 'تغییر رمز ناموفق بود');
      }
    });
  }

  private flash(m: string): void {
    this.toast.set(m);
    setTimeout(() => this.toast.set(''), 2500);
  }
}
