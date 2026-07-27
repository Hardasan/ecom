package com.ecommerce.application.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * @author reza gholamzad
 * @since 6/11/26
 */
@Getter
@RequiredArgsConstructor
public enum ECOMErrorType {

    GENERAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "error.general"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "error.validation"),
    INVALID_TICKET(HttpStatus.BAD_REQUEST, "error.invalid.ticket"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "error.invalid.password"),
    INVALID_SIGNUP_TOKEN(HttpStatus.BAD_REQUEST, "error.invalid.signup.token"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.user.not.found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.user.already.exists"),
    SEND_TICKET_TIME_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "error.ticket.send.time.limit"),
    TICKET_BLOCKED(HttpStatus.FORBIDDEN, "error.ticket.blocked"),
    SMS_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "error.sms.send.failed"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.product.not.found"),
    PRODUCT_URL_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.product.url.already.exists"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "error.category.not.found"),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "error.brand.not.found"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "error.file.upload.failed"),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.cart.item.not.found"),
    PRODUCT_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "error.product.variant.not.found"),
    PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT, "error.product.not.available"),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "error.insufficient.stock"),
    PRODUCT_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "error.product.review.not.found"),
    PRODUCT_REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.product.review.already.exists"),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "error.address.not.found"),
    EMPTY_CART(HttpStatus.CONFLICT, "error.cart.empty"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.order.not.found"),
    ORDER_RESERVATION_EXPIRED(HttpStatus.GONE, "error.order.reservation.expired"),
    ORDER_INVALID_STATUS(HttpStatus.CONFLICT, "error.order.invalid.status"),
    ORDER_PAYMENT_FAILED(HttpStatus.PAYMENT_REQUIRED, "error.order.payment.failed"),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.category.name.already.exists"),
    EXCEL_PARSING_ERROR(HttpStatus.BAD_REQUEST, "error.excel.parsing"),
    EXCEL_EMPTY_FILE(HttpStatus.BAD_REQUEST, "error.excel.empty.file"),
    EXCEL_INVALID_HEADER(HttpStatus.BAD_REQUEST, "error.excel.invalid.header"),
    WISHLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "error.wishlist.item.not.found");

    private final HttpStatus httpStatus;
    private final String messageKey;
}
