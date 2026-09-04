import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toFa } from '../../core/format';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { CartService } from '../../core/cart.service';
import { ConfigService } from '../../core/config.service';

type Step = 'phone' | 'otp' | 'signup';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
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
  // OTP entered as six per-digit boxes; otpCode is their concatenation.
  readonly otpDigits = signal<string[]>(['', '', '', '', '', '']);
  readonly otpCode = computed(() => this.otpDigits().join(''));
  firstName = '';
  lastName = '';
  password = '';
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
            this.otpDigits.set(['', '', '', '', '', '']);
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

  /** Resend countdown as mm:ss (Persian digits), matching the design's «01:30». */
  timerLabel(): string {
    const s = this.ttl();
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return toFa(`${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`);
  }

  /** Masked recipient for the OTP subtitle, e.g. 0912****6789 (Persian digits). */
  maskedPhone(): string {
    const p = this.phone;
    if (p.length < 11) return toFa(p);
    return toFa(`${p.slice(0, 4)}****${p.slice(8)}`);
  }

  /** Handle typing into a single OTP box: keep the last digit and auto-advance. */
  onDigit(event: Event, index: number) {
    const input = event.target as HTMLInputElement;
    const digit = input.value.replace(/\D/g, '').slice(-1);
    const digits = [...this.otpDigits()];
    digits[index] = digit;
    this.otpDigits.set(digits);
    input.value = digit;
    if (digit && index < 5) {
      (input.nextElementSibling as HTMLInputElement | null)?.focus();
    }
  }

  /** Backspace on an empty box steps back to the previous one. */
  onOtpKey(event: KeyboardEvent, index: number) {
    if (event.key === 'Backspace' && !this.otpDigits()[index] && index > 0) {
      const input = event.target as HTMLInputElement;
      (input.previousElementSibling as HTMLInputElement | null)?.focus();
    }
  }

  /** Paste a full code into the boxes at once. */
  onOtpPaste(event: ClipboardEvent) {
    const text = (event.clipboardData?.getData('text') ?? '').replace(/\D/g, '').slice(0, 6);
    if (!text) return;
    event.preventDefault();
    const digits = ['', '', '', '', '', ''];
    for (let i = 0; i < text.length; i++) digits[i] = text[i];
    this.otpDigits.set(digits);
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
    setTimeout(() => document.querySelector<HTMLInputElement>('.otp-box')?.focus());
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
