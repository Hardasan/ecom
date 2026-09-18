const FA_DIGITS = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];

/**
 * Render the Latin digits in a value as Persian digits, leaving every other character untouched.
 * Idempotent (Persian digits pass through), so it is safe to apply to values that are already localized.
 */
export function toFa(value: string | number | null | undefined): string {
  if (value == null) {
    return '';
  }
  return String(value).replace(/[0-9]/g, (d) => FA_DIGITS[+d]);
}

export function toNumber(value: number | string | null | undefined): number {
  if (value == null || value === '') {
    return 0;
  }
  return typeof value === 'number' ? value : Number(value);
}

// Monetary values are stored in Rial (shipping tariff + order totals are computed in Rial);
// shoppers see Toman, so divide by 10 and label تومان everywhere.
export function toToman(value: number | string | null | undefined): number {
  return Math.round(toNumber(value) / 10);
}

/** Toman amount as localized Persian digits WITHOUT the تومان unit (for split amount/unit layouts). */
export function tomanText(value: number | string | null | undefined): string {
  return new Intl.NumberFormat('fa-IR').format(toToman(value));
}

export function formatPrice(value: number | string | null | undefined): string {
  return `${tomanText(value)} تومان`;
}

/**
 * The two prices for a single product line: the amount the shopper pays (`now`) and, when a
 * discount is active, the struck-through original (`was`) plus the rounded percent off. All in Rial.
 */
export function priceParts(prices?: { price: number | string; discountPrice?: number | string | null }[]): {
  now: number;
  was: number | null;
  percentOff: number;
} {
  const first = prices?.[0];
  if (!first) return { now: 0, was: null, percentOff: 0 };
  const price = toNumber(first.price);
  const discount = first.discountPrice != null ? toNumber(first.discountPrice) : 0;
  if (discount > 0 && discount < price) {
    return { now: discount, was: price, percentOff: Math.round(((price - discount) / price) * 100) };
  }
  return { now: price, was: null, percentOff: 0 };
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
// COLOR variants store a colour in variantValue. The admin types it into a free-text field, so besides
// "#RRGGBB" it may lack the '#', use Persian digits, carry an alpha channel, be rgb(…) or a colour
// name. The shopper must always see the actual colour (a swatch) and a Persian name — never the code.

export function isColorVariant(variantType?: string | null): boolean {
  return (variantType ?? '').toUpperCase() === 'COLOR';
}

type Rgb = [number, number, number];

/**
 * Colour names for labels. A stored colour takes the name of its perceptually nearest entry, so every
 * shade gets a sensible name (#1B3A6B → سرمه‌ای), not only the exact codes listed. Some names have
 * several anchors because one hue spans very different shades (pure #000080 vs a muted navy).
 */
const COLOR_NAMES_FA: [hex: string, name: string][] = [
  ['#000000', 'مشکی'], ['#1C1C1C', 'مشکی'],
  ['#36454F', 'ذغالی'],
  ['#5A5A5A', 'خاکستری تیره'],
  ['#808080', 'خاکستری'],
  ['#708090', 'طوسی'], ['#607D8B', 'طوسی'],
  ['#C0C0C0', 'نقره‌ای'],
  ['#D9D9D9', 'خاکستری روشن'],
  ['#FFFFFF', 'سفید'],
  ['#FFFFF0', 'شیری'],
  ['#FFFDD0', 'کرم'],
  ['#F5F5DC', 'بژ'],
  ['#D2B48C', 'نسکافه‌ای'],
  ['#C19A6B', 'شتری'],
  ['#C68E3F', 'عسلی'],
  ['#8B4513', 'قهوه‌ای'], ['#A52A2A', 'قهوه‌ای'], ['#795548', 'قهوه‌ای'],
  ['#4E2A1E', 'شکلاتی'],
  ['#FF0000', 'قرمز'], ['#D32F2F', 'قرمز'],
  ['#800000', 'زرشکی'], ['#7B1E2B', 'زرشکی'],
  ['#B22222', 'آجری'],
  ['#FF7F50', 'مرجانی'],
  ['#FA8072', 'گلبهی'],
  ['#FFC0CB', 'صورتی'], ['#F48FB1', 'صورتی'],
  ['#E91E63', 'سرخابی'], ['#FF1493', 'سرخابی'],
  ['#800080', 'بنفش'], ['#7B3FA0', 'بنفش'],
  ['#C8A2C8', 'یاسی'],
  ['#4B0082', 'نیلی'],
  ['#3B1560', 'بادمجانی'],
  ['#FFA500', 'نارنجی'], ['#F57C00', 'نارنجی'],
  ['#FFFF00', 'زرد'], ['#FBC02D', 'زرد'], ['#FFEB3B', 'زرد'],
  ['#FFD700', 'طلایی'], ['#D4AF37', 'طلایی'],
  ['#E1AD01', 'خردلی'],
  ['#C3B091', 'خاکی'],
  ['#CDDC39', 'لیمویی'],
  ['#00FF00', 'سبز فسفری'],
  ['#008000', 'سبز'], ['#43A047', 'سبز'],
  ['#006400', 'سبز تیره'], ['#1E4D2B', 'سبز تیره'],
  ['#2E8B57', 'سبز یشمی'],
  ['#98FF98', 'سبز نعنایی'],
  ['#90EE90', 'سبز روشن'],
  ['#808000', 'زیتونی'], ['#556B2F', 'زیتونی'], ['#6B8E23', 'زیتونی'],
  ['#0000FF', 'آبی'], ['#1E6FD9', 'آبی'], ['#2B4DA8', 'آبی'],
  ['#87CEEB', 'آبی آسمانی'],
  ['#ADD8E6', 'آبی روشن'],
  ['#000080', 'سرمه‌ای'], ['#1F2F4F', 'سرمه‌ای'], ['#1B2A49', 'سرمه‌ای'],
  ['#008080', 'سبزآبی'],
  ['#40E0D0', 'فیروزه‌ای'], ['#00BCD4', 'فیروزه‌ای'],
  ['#005F6A', 'نفتی']
];

/** English names an admin might type instead of a code. */
const ENGLISH_COLOR_HEX: Record<string, string> = {
  black: '#000000', white: '#FFFFFF', gray: '#808080', grey: '#808080', silver: '#C0C0C0',
  charcoal: '#36454F', ivory: '#FFFFF0', cream: '#FFFDD0', beige: '#F5F5DC', tan: '#D2B48C',
  camel: '#C19A6B', brown: '#8B4513', red: '#FF0000', maroon: '#800000', coral: '#FF7F50',
  salmon: '#FA8072', pink: '#FFC0CB', magenta: '#FF00FF', purple: '#800080', violet: '#EE82EE',
  lilac: '#C8A2C8', lavender: '#E6E6FA', indigo: '#4B0082', orange: '#FFA500', yellow: '#FFFF00',
  gold: '#FFD700', mustard: '#E1AD01', khaki: '#C3B091', lime: '#00FF00', green: '#008000',
  olive: '#808000', mint: '#98FF98', blue: '#0000FF', navy: '#000080', teal: '#008080',
  turquoise: '#40E0D0', cyan: '#00FFFF', skyblue: '#87CEEB'
};

/** Lookup key for a typed colour name: lower-case, no spaces / ZWNJ, Arabic ي ك → Persian ی ک. */
function colorNameKey(value: string): string {
  return value.toLowerCase().replace(/[\s‌]/g, '').replace(/ي/g, 'ی').replace(/ك/g, 'ک');
}

const COLOR_NAME_TO_HEX = new Map<string, string>([
  ...Object.entries(ENGLISH_COLOR_HEX),
  // Persian names too (first anchor wins), so a value typed as «مشکی» still gets its circle.
  ...[...COLOR_NAMES_FA].reverse().map(([hex, name]): [string, string] => [colorNameKey(name), hex])
]);

function hexToRgb(value: string): Rgb | null {
  const m = /^#?([0-9a-f]{3,4}|[0-9a-f]{6}|[0-9a-f]{8})$/i.exec(value);
  if (!m) {
    return null;
  }
  // #abc / #abcd shorthand → #aabbcc(dd); any alpha channel is dropped for the swatch.
  const h = m[1].length <= 4 ? m[1].replace(/./g, (c) => c + c) : m[1];
  return [0, 2, 4].map((i) => parseInt(h.slice(i, i + 2), 16)) as Rgb;
}

/** A stored colour value as RGB, or null when it is not a colour at all. */
function parseColor(value?: string | null): Rgb | null {
  // Persian / Arabic-Indic digits → Latin (a Persian keyboard types «#۱B۳A۶B»).
  const v = (value ?? '')
    .trim()
    .replace(/[۰-۹]/g, (d) => String(d.charCodeAt(0) - 0x06f0))
    .replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660));
  if (!v) {
    return null;
  }
  const hex = hexToRgb(v.replace(/\s+/g, ''));
  if (hex) {
    return hex;
  }
  const rgb = /^rgba?\(\s*(\d{1,3})\s*[,\s]\s*(\d{1,3})\s*[,\s]\s*(\d{1,3})/i.exec(v);
  if (rgb) {
    return [rgb[1], rgb[2], rgb[3]].map((c) => Math.min(255, Number(c))) as Rgb;
  }
  const named = COLOR_NAME_TO_HEX.get(colorNameKey(v));
  return named ? hexToRgb(named) : null;
}

/** The stored value as a CSS color ("#RRGGBB"), or '' when it is not a colour. */
export function colorHex(value?: string | null): string {
  const rgb = parseColor(value);
  return rgb ? '#' + rgb.map((c) => c.toString(16).padStart(2, '0')).join('').toUpperCase() : '';
}

/** sRGB → CIE Lab (D65); distance in Lab follows perceived colour difference, unlike raw RGB. */
function toLab([r, g, b]: Rgb): [number, number, number] {
  const lin = (c: number) => {
    c /= 255;
    return c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  };
  const [R, G, B] = [lin(r), lin(g), lin(b)];
  const f = (t: number) => (t > 216 / 24389 ? Math.cbrt(t) : (841 / 108) * t + 4 / 29);
  const x = f((0.4124 * R + 0.3576 * G + 0.1805 * B) / 0.95047);
  const y = f(0.2126 * R + 0.7152 * G + 0.0722 * B);
  const z = f((0.0193 * R + 0.1192 * G + 0.9505 * B) / 1.08883);
  return [116 * y - 16, 500 * (x - y), 200 * (y - z)];
}

let paletteLab: { name: string; lab: [number, number, number] }[] | null = null;

/** Persian name of the perceptually nearest palette colour. */
function nearestColorName(rgb: Rgb): string {
  paletteLab ??= COLOR_NAMES_FA.map(([hex, name]) => ({ name, lab: toLab(hexToRgb(hex)!) }));
  const [l, a, b] = toLab(rgb);
  let best = paletteLab[0];
  let bestDistance = Infinity;
  for (const entry of paletteLab) {
    const d = (l - entry.lab[0]) ** 2 + (a - entry.lab[1]) ** 2 + (b - entry.lab[2]) ** 2;
    if (d < bestDistance) {
      bestDistance = d;
      best = entry;
    }
  }
  return best.name;
}

/**
 * Whether a variant value is a colour. Return items carry no variantType, so a value of unknown type
 * counts only when written as a code ('#…') — a bare "100" could just as well be a size.
 */
function isColorValue(variantType: string | null | undefined, value: string): boolean {
  return isColorVariant(variantType) || (!variantType && value.startsWith('#'));
}

/** CSS colour for a variant line's swatch dot, or '' when the variant is not a colour. */
export function variantSwatch(variantType?: string | null, variantValue?: string | null): string {
  const v = (variantValue ?? '').trim();
  return isColorValue(variantType, v) ? colorHex(v) : '';
}

/**
 * Human label for a variant value: a Persian colour name for colours (never the raw code), the raw
 * value (e.g. a size) otherwise. Empty when there is no variant.
 */
export function variantLabel(variantType?: string | null, variantValue?: string | null): string {
  const v = (variantValue ?? '').trim();
  if (!v) {
    return '';
  }
  if (isColorValue(variantType, v)) {
    const rgb = parseColor(v);
    if (rgb) {
      return nearestColorName(rgb);
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
