export function toNumber(value: number | string | null | undefined): number {
  if (value == null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

export function formatPrice(value: number | string | null | undefined, unit = 'تومان'): string {
  const n = toNumber(value);
  return `${new Intl.NumberFormat('fa-IR').format(n)} ${unit}`;
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
  FAILED: 'ناموفق',
  SENDING: 'در حال ارسال',
  RECEIVED: 'تحویل شده',
  CANCEL_BY_USER: 'لغو شده',
  CANCEL_BY_ADMIN: 'لغو شده'
};

export function orderStatusLabel(status?: string | null): string {
  if (!status) {
    return '';
  }
  return ORDER_STATUS_FA[status] ?? status;
}
