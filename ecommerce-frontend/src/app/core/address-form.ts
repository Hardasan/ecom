/**
 * Shared client-side validation for the address form (used by both the profile page and the
 * checkout sheet). Returns a map of field → Persian error message; an empty map means valid.
 * The backend also validates (@NotBlank/@NotNull), but doing it here lets us mark the exact
 * empty/invalid fields inline instead of silently doing nothing.
 */
export function validateAddressFields(values: {
  recipientFirstName: string;
  recipientLastName: string;
  recipientMobile: string;
  city: string;
  postalCode: string;
  addressLine: string;
}): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!values.recipientFirstName.trim()) {
    errors['recipientFirstName'] = 'نام گیرنده را وارد کنید';
  }
  if (!values.recipientLastName.trim()) {
    errors['recipientLastName'] = 'نام خانوادگی گیرنده را وارد کنید';
  }

  const mobile = values.recipientMobile.replace(/\D/g, '');
  if (!mobile) {
    errors['recipientMobile'] = 'شماره موبایل گیرنده را وارد کنید';
  } else if (!/^09\d{9}$/.test(mobile)) {
    errors['recipientMobile'] = 'شماره موبایل معتبر نیست (مثال: ۰۹۱۲۳۴۵۶۷۸۹)';
  }

  if (!values.city.trim()) {
    errors['city'] = 'شهر را انتخاب کنید';
  }

  const postal = values.postalCode.replace(/\D/g, '');
  if (!postal) {
    errors['postalCode'] = 'کد پستی را وارد کنید';
  } else if (postal.length !== 10) {
    errors['postalCode'] = 'کد پستی باید ۱۰ رقم باشد';
  }

  if (!values.addressLine.trim()) {
    errors['addressLine'] = 'آدرس کامل را وارد کنید';
  }

  return errors;
}
