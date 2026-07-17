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
    TOO_MANY_REQUEST(HttpStatus.TOO_MANY_REQUESTS, "error.too.many.requests"),
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
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "error.address.not.found"),
    EMPTY_CART(HttpStatus.CONFLICT, "error.cart.empty"),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "error.order.not.found"),
    ORDER_RESERVATION_EXPIRED(HttpStatus.GONE, "error.order.reservation.expired"),
    CATEGORY_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "error.category.name.already.exists");

    private final HttpStatus httpStatus;
    private final String messageKey;
}
