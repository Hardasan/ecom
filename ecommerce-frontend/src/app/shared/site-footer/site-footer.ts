import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ASSETS } from '../../assets';

type FooterLink = { label: string; route?: string };
type FooterSection = { key: string; title: string; links: FooterLink[] };

/**
 * Storefront footer (design screen «فوتر»): brand blurb + socials, three collapsible link sections,
 * and the copyright line. Rendered on the phone home (below the feed) and by {@link App} on desktop.
 * Sections are accordions on the phone and shown expanded in columns on desktop (CSS-driven).
 */
@Component({
  selector: 'app-site-footer',
  imports: [RouterLink],
  templateUrl: './site-footer.html',
  styleUrl: './site-footer.scss'
})
export class SiteFooter {
  readonly a = ASSETS;

  // Jalali (Persian-calendar) year, e.g. ۱۴۰۵ — matches the design's «© ۱۴۰۵».
  readonly year = new Intl.DateTimeFormat('fa-IR', { year: 'numeric' })
    .format(new Date())
    .replace(/[^۰-۹0-9]/g, '');

  readonly sections: FooterSection[] = [
    {
      key: 'customer',
      title: 'خدمات مشتریان',
      links: [
        { label: 'پاسخ به پرسش‌های متداول' },
        { label: 'رویه‌های بازگرداندن کالا', route: '/returns' },
        { label: 'شرایط استفاده و حریم خصوصی' },
        { label: 'رهگیری سفارش‌های ارسالی', route: '/orders' }
      ]
    },
    {
      key: 'guide',
      title: 'راهنمای خرید',
      links: [
        { label: 'نحوه ثبت سفارش نهایی' },
        { label: 'روش‌های پرداخت و تسویه حساب' },
        { label: 'شیوه‌های ارسال بار و مرسولات' }
      ]
    },
    {
      key: 'contact',
      title: 'ارتباط با ما',
      links: [
        { label: 'تلفن پشتیبانی: ۰۲۱-۱۲۳۴۵۶' },
        { label: 'ایمیل: support@rivany.ir' },
        { label: 'آدرس: تهران، خیابان مطهری، پلاک ۱۱۰' }
      ]
    }
  ];

  // Which accordion panels are open (phone only — desktop shows them all via CSS).
  readonly open = signal<Set<string>>(new Set());

  toggle(key: string): void {
    const next = new Set(this.open());
    next.has(key) ? next.delete(key) : next.add(key);
    this.open.set(next);
  }

  isOpen(key: string): boolean {
    return this.open().has(key);
  }
}
