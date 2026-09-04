import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../api.config';
import { UserProfileDto } from './models';

const TOKEN_KEY = 'rivani_token';
const ROLE_KEY = 'rivani_role';

export type LoginResponse = { token: string; role: string };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenSignal = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  private readonly roleSignal = signal<string | null>(localStorage.getItem(ROLE_KEY));

  readonly token = this.tokenSignal.asReadonly();
  readonly role = this.roleSignal.asReadonly();
  readonly isLoggedIn = computed(() => !!this.tokenSignal());

  constructor(private readonly http: HttpClient) {}

  checkRegistration(mobileNumber: string): Observable<{ registered?: boolean; isRegistered?: boolean }> {
    return this.http.post<{ registered?: boolean; isRegistered?: boolean }>(
      `${API_BASE_URL}/user/check-registration`,
      { mobileNumber }
    );
  }

  sendLoginTicket(mobileNumber: string): Observable<{ ticketTTLInSecond: number }> {
    return this.http.post<{ ticketTTLInSecond: number }>(`${API_BASE_URL}/user/login-ticket`, {
      mobileNumber
    });
  }

  validateLoginTicket(mobileNumber: string, ticket: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${API_BASE_URL}/user/login-ticket/validation`, { mobileNumber, ticket })
      .pipe(tap((res) => this.persist(res)));
  }

  sendSignupTicket(mobileNumber: string): Observable<{ ticketTTLInSecond: number }> {
    return this.http.post<{ ticketTTLInSecond: number }>(`${API_BASE_URL}/user/signup-ticket`, {
      mobileNumber
    });
  }

  validateSignupTicket(
    mobileNumber: string,
    ticket: string
  ): Observable<{ signupToken: string }> {
    return this.http.post<{ signupToken: string }>(`${API_BASE_URL}/user/signup-ticket/validation`, {
      mobileNumber,
      ticket
    });
  }

  signup(body: {
    signupToken: string;
    password: string;
    firstName: string;
    lastName: string;
  }): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/user/signup`, body);
  }

  login(mobileNumber: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${API_BASE_URL}/user/login`, { mobileNumber, password })
      .pipe(tap((res) => this.persist(res)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    this.tokenSignal.set(null);
    this.roleSignal.set(null);
  }

  getProfile(): Observable<UserProfileDto> {
    return this.http.get<UserProfileDto>(`${API_BASE_URL}/user/me`);
  }

  updateProfile(body: UserProfileDto): Observable<UserProfileDto> {
    return this.http.put<UserProfileDto>(`${API_BASE_URL}/user/me`, body);
  }

  /** The shopper's saved شبا (IBAN), used to prefill the returns refund form. */
  getIban(): Observable<{ iban: string | null }> {
    return this.http.get<{ iban: string | null }>(`${API_BASE_URL}/user/iban`);
  }

  private persist(res: LoginResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    localStorage.setItem(ROLE_KEY, res.role);
    this.tokenSignal.set(res.token);
    this.roleSignal.set(res.role);
  }
}
