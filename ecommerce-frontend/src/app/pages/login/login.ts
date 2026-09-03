import { Component, DestroyRef, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FaNumPipe } from '../../core/fa-num.pipe';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { ConfigService } from '../../core/config.service';

type Step = 'phone' | 'otp' | 'signup';

@Component({
  selector: 'app-login',
  imports: [FormsModule, FaNumPipe],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly cartApi = inject(CartService);
  private readonly configApi = inject(ConfigService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly step = signal<Step>('phone');
  readonly busy = signal(false);
  readonly error = signal('');
  readonly ttl = signal(0);

  phone = '';
  readonly otpCode = signal('');
  firstName = '';
  lastName = '';
  password = '';
  private readonly otpInput = viewChild<ElementRef<HTMLInputElement>>('otpInput');
  private registered = false;
  private signupToken = '';
  private returnUrl = '/';
  private timerId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => this.stopTimer());
  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
    this.configApi.load().subscribe({ error: () => undefined });
    if (this.auth.isLoggedIn()) {
      void this.router.navigateByUrl(this.returnUrl);
    }
  }

  sendCode() {
    const mobileNumber = this.normalizePhone(this.phone);
    if (!/^09\d{9}$/.test(mobileNumber)) {
      this.error.set('شماره موبایل معتبر نیست (مثال: ۰۹۱۲۳۴۵۶۷۸۹)');
      return;
    }
    this.phone = mobileNumber;
    this.busy.set(true);
    this.error.set('');

    this.auth.checkRegistration(mobileNumber).subscribe({
      next: (res) => {
        this.registered = !!(res.registered ?? res.isRegistered);
        const req$ = this.registered
          ? this.auth.sendLoginTicket(mobileNumber)
          : this.auth.sendSignupTicket(mobileNumber);
        req$.subscribe({
          next: (ticket) => {
            this.startTimer(ticket.ticketTTLInSecond ?? this.configApi.otpTtlSeconds());
            this.otpCode.set('');
            this.step.set('otp');
            this.busy.set(false);
            this.focusOtp();
          },
          error: (err) => this.fail(err)
        });
      },
      error: (err) => this.fail(err)
    });
  }

  resendCode() {
    if (this.ttl() > 0 || this.busy()) {
      return;
    }
    this.sendCode();
  }

  editPhone() {
    this.stopTimer();
    this.step.set('phone');
    this.error.set('');
  }

  onOtpCodeInput(event: Event) {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/\D/g, '').slice(0, 6);
    this.otpCode.set(digits);
    input.value = digits;
  }

  confirmOtp() {
    const ticket = this.otpCode();
    if (ticket.length !== 6) {
      this.error.set('کد ۶ رقمی را کامل وارد کنید');
      return;
    }
    this.busy.set(true);
    this.error.set('');

    if (this.registered) {
      this.auth.validateLoginTicket(this.phone, ticket).subscribe({
        next: () => this.finishLogin(),
        error: (err) => this.fail(err)
      });
      return;
    }

    this.auth.validateSignupTicket(this.phone, ticket).subscribe({
      next: (res) => {
        this.signupToken = res.signupToken;
        this.stopTimer();
        this.step.set('signup');
        this.busy.set(false);
      },
      error: (err) => this.fail(err)
    });
  }

  completeSignup() {
    if (!this.firstName.trim() || !this.lastName.trim() || this.password.length < 6) {
      this.error.set('نام، نام خانوادگی و رمز حداقل ۶ کاراکتر لازم است');
      return;
    }
    this.busy.set(true);
    this.error.set('');
    this.auth
      .signup({
        signupToken: this.signupToken,
        firstName: this.firstName.trim(),
        lastName: this.lastName.trim(),
        password: this.password
      })
      .subscribe({
        next: () => {
          this.auth.login(this.phone, this.password).subscribe({
            next: () => this.finishLogin(),
            error: (err) => this.fail(err)
          });
        },
        error: (err) => this.fail(err)
      });
  }

  /** Adopt the guest cart into the account, then continue to where the user was headed. */
  private finishLogin() {
    this.cartApi.onLogin().subscribe({
      next: () => {
        this.busy.set(false);
        void this.router.navigateByUrl(this.returnUrl);
      }
    });
  }

  private focusOtp() {
    setTimeout(() => this.otpInput()?.nativeElement.focus());
  }

  private startTimer(seconds: number) {
    this.stopTimer();
    this.ttl.set(seconds);
    this.timerId = setInterval(() => {
      const next = this.ttl() - 1;
      this.ttl.set(Math.max(next, 0));
      if (next <= 0) {
        this.stopTimer();
      }
    }, 1000);
  }

  private stopTimer() {
    if (this.timerId != null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  private normalizePhone(value: string): string {
    return value.replace(/[^\d]/g, '');
  }

  private fail(err: { error?: { message?: string } }) {
    this.busy.set(false);
    this.error.set(err?.error?.message ?? 'خطا در ارتباط با سرور');
  }
}
