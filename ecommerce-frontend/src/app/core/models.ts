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
  // Review aggregate for card/list display (added to the product-list projection in the redesign).
  // Optional so existing callers/endpoints that don't populate it still type-check.
  averageRating?: number | string | null;
  ratingCount?: number | null;
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

export type PaymentMethod = 'ONLINE' | 'CASH_ON_DELIVERY';

// ---- Returns (مرجوعی) ---------------------------------------------------------------------------
export type ReturnReason =
  | 'SIZE_OR_COLOR_MISMATCH'
  | 'DEFECTIVE'
  | 'NOT_AS_DESCRIBED'
  | 'CHANGED_MIND'
  | 'OTHER';

export type ReturnStatus = 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'REFUNDED';

export type ReturnItemDto = {
  orderItemId: number;
  productName: string;
  variantValue?: string | null;
  quantity: number;
  unitPrice: number | string;
  lineRefund: number | string;
  reason: ReturnReason;
};

export type ReturnRequestDto = {
  id: number;
  orderId: number;
  status: ReturnStatus;
  refundAmount: number | string;
  iban?: string | null;
  note?: string | null;
  items: ReturnItemDto[];
  createdAt?: string;
  updatedAt?: string;
};

export type CreateReturnBody = {
  orderId: number;
  note?: string | null;
  iban?: string | null;
  items: { orderItemId: number; quantity: number; reason: ReturnReason }[];
};

export type CheckoutQuoteDto = {
  itemsCost: number | string;
  shippingCost: number | string;
  totalCost: number | string;
};

export type DiscountPreviewDto = {
  code: string;
  type: string;
  itemsCost: number | string;
  eligibleSubtotal: number | string;
  discountAmount: number | string;
  newItemsCost: number | string;
};

export type OrderDto = {
  id: number;
  userId: number;
  status: string;
  paymentMethod?: PaymentMethod;
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
  discountCode?: string | null;
  discountAmount?: number | string | null;
  totalWeightGram?: number | null;
  shippingZone?: string | null;
  reservedUntil?: string | null;
  recipientNationalId?: string | null;
  // Fulfillment snapshot (populated by warehouse staff as the order advances).
  carrier?: string | null;
  trackingNumber?: string | null;
  approvedAt?: string | null;
  shippedAt?: string | null;
  deliveredAt?: string | null;
  fulfilledByUserId?: number | null;
  createdAt?: string;
  updatedAt?: string;
  transactions?: TransactionDto[];
};

export type TransactionDto = {
  id: number;
  type: 'PAYMENT' | 'REFUND' | string;
  amount: number | string;
  reference?: string | null;
  iban?: string | null;
  createdAt?: string;
};

export type ReviewSummaryDto = {
  productId: number;
  averageRating: number | string;
  totalCount: number;
  // Zero-filled 1..5 histogram from the backend, for the reviews-tab bar chart.
  ratingCounts?: Record<number, number>;
};

export type ReviewDto = {
  id: number;
  productId: number;
  authorName: string;
  rating: number;
  title?: string | null;
  comment?: string | null;
  verifiedPurchase?: boolean;
  status?: string;
  createdAt?: string;
  updatedAt?: string;
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

// ---- Admin domain types -------------------------------------------------------------------------

export type ProductStatus = 'ACTIVE' | 'INACTIVE';
export type InventoryStatus = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
export type VariantTypeValue = 'COLOR' | 'SIZE';
export type DiscountType = 'PERCENTAGE' | 'FIXED_AMOUNT';
export type DiscountScope = 'ALL' | 'PRODUCTS' | 'CATEGORIES';
export type ReviewStatus = 'PENDING' | 'PUBLISHED' | 'HIDDEN';
export type ImageType = 'MAIN' | 'OTHER';

export const SPEC_KEYS = [
  'COLOR', 'SIZE', 'WEIGHT', 'MATERIAL', 'DIMENSIONS', 'CONNECTIVITY', 'POWER_CONSUMPTION',
  'WARRANTY', 'COUNTRY_OF_ORIGIN', 'MODEL_NUMBER', 'PROCESSOR', 'MEMORY', 'STORAGE',
  'DISPLAY_SIZE', 'OPERATING_SYSTEM'
] as const;
export type SpecificationKey = (typeof SPEC_KEYS)[number];

/** Payload for POST/PUT /api/products (mirrors the server CreateProductRequestDto). */
export type ProductWriteDto = {
  categoryId: number;
  subCategoryId?: number | null;
  url: string;
  variantType?: VariantTypeValue | null;
  prices: PriceDto[];
  shortDescription?: string | null;
  fullDescription?: string | null;
  specification?: Record<string, string>;
  name: string;
  localName?: string | null;
  brandId?: number | null;
  inventoryStatus: InventoryStatus;
  status: ProductStatus;
  inventoryCount: number;
  weightGram?: number | null;
};

export type DiscountDto = {
  id?: number;
  code: string;
  type: DiscountType;
  value: number | string;
  maxDiscountAmount?: number | string | null;
  minimumCartAmount?: number | string | null;
  scope: DiscountScope;
  productIds?: number[] | null;
  categoryIds?: number[] | null;
  expiresAt?: string | null;
  usageLimit?: number | null;
  usageCount?: number | null;
  perUserLimit?: number | null;
  createdAt?: string;
  updatedAt?: string;
};

export type BatchRowErrorDto = { rowNumber: number; field: string; message: string };
export type BatchUploadResultDto = {
  totalRows: number;
  successCount: number;
  createdCount: number;
  updatedCount: number;
  failureCount: number;
  errors: BatchRowErrorDto[];
  elapsedTimeMs: number;
};

export type AdminReviewDto = {
  id: number;
  productId: number;
  productName?: string;
  productLocalName?: string | null;
  productCode?: string;
  authorName: string;
  rating: number;
  title?: string | null;
  comment?: string | null;
  verifiedPurchase?: boolean;
  status: ReviewStatus;
  createdAt?: string;
  updatedAt?: string;
};

export type AdminStatsDto = {
  totalOrders: number;
  ordersByStatus: Record<string, number>;
  totalRevenue: number | string;
  awaitingShipment: number;
  processingOrders: number;
  reservedOrders: number;
  refundableOrders: number;
  totalProducts: number;
  activeProducts: number;
  outOfStockProducts: number;
  totalCategories: number;
  totalDiscounts: number;
  pendingReviews: number;
};

/** A warehouse-staff account as returned by the admin staff-management API. */
export type StaffDto = {
  id: number;
  firstName: string;
  lastName: string;
  mobile: string;
  enabled: boolean;
  createdAt?: string;
};

export type CreateStaffPayload = {
  firstName: string;
  lastName: string;
  mobile: string;
  password: string;
};
