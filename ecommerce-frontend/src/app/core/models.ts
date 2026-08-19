export type PriceDto = {
  price: number | string;
  discountPrice?: number | string | null;
  variantValue?: string | null;
};

export type ProductImageDto = {
  altText?: string | null;
  imageData?: string | null;
};

export type ClientConfigDto = {
  otpTtlSeconds: number;
};

export type UserProfileDto = {
  firstName: string;
  lastName: string;
  mobile: string;
};

export type ProductDto = {
  id: number;
  code?: string;
  name?: string;
  localName?: string;
  categoryId?: number;
  subCategoryId?: number;
  brandId?: number;
  url?: string;
  variantType?: 'COLOR' | 'SIZE' | null;
  prices?: PriceDto[];
  shortDescription?: string;
  fullDescription?: string;
  specification?: Record<string, string>;
  mainImage?: ProductImageDto | null;
  otherImages?: { id?: number; altText?: string; imageData?: string }[];
  inventoryStatus?: string;
  status?: string;
  inventoryCount?: number;
  weightGram?: number;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type CategoryDto = {
  id: number;
  name: string;
  localName: string;
  parentId?: number | null;
};

export type CategoryHierarchyItem = {
  category: CategoryDto;
  subCategories: CategoryDto[];
};

export type CartItemDto = {
  id: number;
  productId: number;
  productName: string;
  productCode?: string;
  mainImage?: ProductImageDto | null;
  variantType?: string | null;
  variantValue?: string | null;
  quantity: number;
  unitPrice: number | string;
  discountPrice?: number | string | null;
  effectivePrice: number | string;
  lineTotal: number | string;
};

export type CartDto = {
  userId: number;
  items: CartItemDto[];
  totalQuantity: number;
  totalPrice: number | string;
};

export type AddressDto = {
  id?: number;
  title?: string;
  recipientFirstName: string;
  recipientLastName: string;
  recipientMobile: string;
  recipientNationalId?: string;
  province: string;
  city: string;
  postalCode: string;
  addressLine: string;
  plaque?: string;
  unit?: string;
  isDefault?: boolean;
};

export type OrderItemDto = {
  id: number;
  productId: number;
  productName: string;
  productCode?: string;
  mainImage?: ProductImageDto | null;
  variantType?: string | null;
  variantValue?: string | null;
  quantity: number;
  unitPrice: number | string;
  discountPrice?: number | string | null;
  lineTotal: number | string;
};

export type OrderDto = {
  id: number;
  userId: number;
  status: string;
  recipientFirstName?: string;
  recipientLastName?: string;
  recipientMobile?: string;
  province?: string;
  city?: string;
  postalCode?: string;
  addressLine?: string;
  plaque?: string;
  unit?: string;
  items: OrderItemDto[];
  itemsCost: number | string;
  shippingCost: number | string;
  totalCost: number | string;
  createdAt?: string;
  transactions?: { id: number; type: string; amount: number | string; reference?: string }[];
};

export type ReviewSummaryDto = {
  productId: number;
  averageRating: number | string;
  totalCount: number;
};

export type PaymentInitiationDto = {
  paymentReference: string;
  redirectUrl: string;
};

export type GeoProvinceDto = {
  code: string;
  name: string;
};

export type GeoCityDto = {
  id: number;
  name: string;
};
