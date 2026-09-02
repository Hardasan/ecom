export function toNumber(value: number | string | null | undefined): number {
  if (value == null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

// Monetary values are stored in Rial (shipping tariff + order totals are computed in Rial);
// shoppers see Toman, so divide by 10 and label تومان everywhere.
export function formatPrice(value: number | string | null | undefined): string {
  const toman = Math.round(toNumber(value) / 10);
  return `${new Intl.NumberFormat('fa-IR').format(toman)} تومان`;
}

export function imageSrc(image?: { imageData?: string | null } | string | null): string {
  const data = typeof image === 'string' ? image : image?.imageData;
  if (!data) {
    return '';
  }
  if (data.startsWith('data:') || data.startsWith('http') || data.startsWith('/')) {
    return data;
  }
  if (data.startsWith('iVBOR')) {
    return `data:image/png;base64,${data}`;
  }
  if (data.startsWith('UklGR')) {
    return `data:image/webp;base64,${data}`;
  }
  if (data.startsWith('/9j/')) {
    return `data:image/jpeg;base64,${data}`;
  }
  return `data:image/jpeg;base64,${data}`;
}

export function productImageSrc(product?: {
  mainImage?: { imageData?: string | null } | null;
} | null): string {
  return imageSrc(product?.mainImage);
}

export function effectiveUnitPrice(prices?: { price: number | string; discountPrice?: number | string | null }[]): number {
  const first = prices?.[0];
  if (!first) {
    return 0;
  }
  const discount = first.discountPrice != null ? toNumber(first.discountPrice) : 0;
  return discount > 0 ? discount : toNumber(first.price);
}

export function displayName(product: { localName?: string; name?: string }): string {
  return product.localName || product.name || 'محصول';
}

// ---- Variant display -----------------------------------------------------------------------------
// COLOR variants store a CSS hex code (e.g. "#FFFFFF") in variantValue. The shopper must see the
// actual color (a swatch) — and a readable name where we know it — never the raw hex string.

export function isColorVariant(variantType?: string | null): boolean {
  return (variantType ?? '').toUpperCase() === 'COLOR';
}

const HEX_RE = /^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;

/** The stored value as a CSS color ("#RRGGBB"), or '' when it is not a hex code. */
export function colorHex(value?: string | null): string {
  const v = (value ?? '').trim();
  if (!HEX_RE.test(v)) {
    return '';
  }
  return v.startsWith('#') ? v : `#${v}`;
}

const COLOR_NAMES_FA: Record<string, string> = {
  '#FFFFFF': 'سفید',
  '#000000': 'مشکی',
  '#FF0000': 'قرمز',
  '#00FF00': 'سبز',
  '#008000': 'سبز',
  '#0000FF': 'آبی',
  '#FFFF00': 'زرد',
  '#FFA500': 'نارنجی',
  '#800080': 'بنفش',
  '#FFC0CB': 'صورتی',
  '#A52A2A': 'قهوه‌ای',
  '#808080': 'خاکستری',
  '#C0C0C0': 'نقره‌ای',
  '#FFD700': 'طلایی',
  '#008080': 'فیروزه‌ای',
  '#000080': 'سرمه‌ای'
};

/**
 * Human label for a variant value: a color name (falling back to the hex) for COLOR variants,
 * the raw value (e.g. a size) otherwise. Empty when there is no variant.
 */
export function variantLabel(variantType?: string | null, variantValue?: string | null): string {
  const v = (variantValue ?? '').trim();
  if (!v) {
    return '';
  }
  if (isColorVariant(variantType)) {
    const hex = colorHex(v);
    if (hex) {
      return COLOR_NAMES_FA[hex.toUpperCase()] ?? hex;
    }
  }
  return v;
}

export function formatFaDate(value?: string | Date | null): string {
  if (!value) {
    return '';
  }
  try {
    return new Intl.DateTimeFormat('fa-IR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }).format(new Date(value));
  } catch {
    return '';
  }
}

export function orderItemCount(items?: { quantity: number }[] | null): number {
  return (items ?? []).reduce((sum, item) => sum + (item.quantity || 0), 0);
}

const ORDER_STATUS_FA: Record<string, string> = {
  RESERVED: 'رزرو شده',
  PAID: 'پرداخت شده',
  PROCESSING: 'در حال آماده‌سازی',
  FAILED: 'ناموفق',
  SENDING: 'در حال ارسال',
  RECEIVED: 'تحویل شده',
  CANCEL_BY_USER: 'لغو شده',
  CANCEL_BY_ADMIN: 'لغو شده'
};

export function orderStatusLabel(status?: string | null, paymentMethod?: string | null): string {
  if (!status) {
    return '';
  }
  // A cash-on-delivery order sits at RESERVED until an admin ships it — there is no "pending payment"
  // for the shopper, so label it as awaiting shipment rather than "reserved".
  if (status === 'RESERVED' && paymentMethod === 'CASH_ON_DELIVERY') {
    return 'در انتظار ارسال';
  }
  return ORDER_STATUS_FA[status] ?? status;
}
