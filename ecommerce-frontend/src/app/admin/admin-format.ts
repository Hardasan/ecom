import { DiscountScope, DiscountType, ReviewStatus } from '../core/models';

/** Persian labels + badge tones for admin-only enums (order labels live in core/format.ts). */

const REVIEW_STATUS_FA: Record<ReviewStatus, string> = {
  PENDING: 'در انتظار تأیید',
  PUBLISHED: 'منتشر شده',
  HIDDEN: 'پنهان'
};

export function reviewStatusLabel(status?: ReviewStatus | string | null): string {
  return status ? (REVIEW_STATUS_FA[status as ReviewStatus] ?? status) : '';
}

export function discountTypeLabel(type?: DiscountType | string | null): string {
  return type === 'PERCENTAGE' ? 'درصدی' : type === 'FIXED_AMOUNT' ? 'مبلغ ثابت' : (type ?? '');
}

export function discountScopeLabel(scope?: DiscountScope | string | null): string {
  switch (scope) {
    case 'ALL':
      return 'کل سبد خرید';
    case 'PRODUCTS':
      return 'محصولات منتخب';
    case 'CATEGORIES':
      return 'دسته‌های منتخب';
    default:
      return scope ?? '';
  }
}

/** Badge tone class for an order status. */
export function orderStatusTone(status?: string | null): string {
  switch (status) {
    case 'PAID':
      return 'badge--green';
    case 'SENDING':
      return 'badge--blue';
    case 'RECEIVED':
      return 'badge--green';
    case 'RESERVED':
      return 'badge--amber';
    case 'FAILED':
    case 'CANCEL_BY_USER':
    case 'CANCEL_BY_ADMIN':
      return 'badge--red';
    default:
      return 'badge--gray';
  }
}

export function reviewStatusTone(status?: ReviewStatus | string | null): string {
  switch (status) {
    case 'PUBLISHED':
      return 'badge--green';
    case 'PENDING':
      return 'badge--amber';
    case 'HIDDEN':
      return 'badge--gray';
    default:
      return 'badge--gray';
  }
}

export const ORDER_STATUSES = [
  'RESERVED', 'PAID', 'SENDING', 'RECEIVED', 'FAILED', 'CANCEL_BY_USER', 'CANCEL_BY_ADMIN'
] as const;
