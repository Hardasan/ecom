package com.ecommerce.application.service.order;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.address.AddressResponseDto;
import com.ecommerce.application.api.dto.order.GuestCheckoutRequestDto;
import com.ecommerce.application.api.dto.order.GuestItemRequestDto;
import com.ecommerce.application.api.dto.order.OrderResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.application.service.shipping.ShippingResult;
import com.ecommerce.persistence.entity.AppUser;
import com.ecommerce.persistence.entity.enumeration.OrderStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.ShippingZone;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutService_guestCheckoutUTest extends BaseCheckoutServiceUTest {

    private static final String GUEST_MOBILE = "09129998877";

    private GuestCheckoutRequestDto request() {
        GuestCheckoutRequestDto dto = new GuestCheckoutRequestDto();
        dto.setFirstName("Sara");
        dto.setLastName("Ahmadi");
        dto.setMobile(GUEST_MOBILE);
        dto.setEmail("sara@example.com");

        AddressRequestDto addressDto = new AddressRequestDto();
        addressDto.setRecipientFirstName("Sara");
        addressDto.setRecipientLastName("Ahmadi");
        addressDto.setRecipientMobile(GUEST_MOBILE);
        addressDto.setProvince(Province.TEHRAN);
        addressDto.setCity("Tehran");
        addressDto.setPostalCode("1234567890");
        addressDto.setAddressLine("Valiasr St");
        dto.setAddress(addressDto);

        GuestItemRequestDto item = new GuestItemRequestDto();
        item.setProductId(PRODUCT_ID);
        item.setVariantType(VariantType.COLOR);
        item.setVariantValue(DEFAULT_VARIANT_VALUE);
        item.setQuantity(2);
        dto.setItems(List.of(item));
        return dto;
    }

    private void stubOrderPlacement() {
        AddressResponseDto created = new AddressResponseDto();
        created.setId(ADDRESS_ID);
        when(addressService.create(eq(USER_ID), any(AddressRequestDto.class))).thenReturn(created);
        when(userAddressRepository.findByIdAndUserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address()));
        when(productRepository.findById(PRODUCT_ID))
                .thenReturn(Optional.of(product(10, 500, ProductStatus.ACTIVE)));
        when(shippingCalculator.calculate(eq(Province.TEHRAN), anyInt()))
                .thenReturn(new ShippingResult(ShippingZone.INTRA_PROVINCE, BigDecimal.valueOf(183000)));
    }

    @Test
    void guest_checkout_creates_unregistered_user_places_order_and_decrements_inventory() {
        when(appUserRepository.findByMobile(GUEST_MOBILE)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });
        stubOrderPlacement();

        OrderResponseDto response = checkoutService.guestCheckout(request());

        assertEquals(OrderStatus.PENDING, response.getStatus());
        // catalog price 100 * qty 2 = 200
        assertEquals(0, response.getItemsCost().compareTo(BigDecimal.valueOf(200)));
        assertEquals(ShippingZone.INTRA_PROVINCE, response.getShippingZone());
        assertNotNull(response.getReservedUntil());

        verify(productRepository).decrementInventory(PRODUCT_ID, 2);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser saved = userCaptor.getValue();
        assertFalse(saved.getIsRegistered());
        assertEquals(GUEST_MOBILE, saved.getMobile());
        assertEquals(GUEST_MOBILE, saved.getUsername());
    }

    @Test
    void guest_checkout_with_registered_mobile_is_rejected() {
        AppUser registered = new AppUser();
        registered.setId(USER_ID);
        registered.setIsRegistered(true);
        when(appUserRepository.findByMobile(GUEST_MOBILE)).thenReturn(Optional.of(registered));

        EcommerceException exception = assertThrows(EcommerceException.class,
                () -> checkoutService.guestCheckout(request()));

        assertEquals(ECOMErrorType.USER_ALREADY_EXISTS, exception.getEcomErrorType());
        verify(appUserRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void guest_checkout_reuses_existing_unregistered_user_without_resetting_password() {
        AppUser existingGuest = new AppUser();
        existingGuest.setId(USER_ID);
        existingGuest.setIsRegistered(false);
        existingGuest.setPassword("existing-hash");
        when(appUserRepository.findByMobile(GUEST_MOBILE)).thenReturn(Optional.of(existingGuest));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubOrderPlacement();

        checkoutService.guestCheckout(request());

        verify(passwordEncoder, never()).encode(any());
        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        assertEquals("existing-hash", userCaptor.getValue().getPassword());
        assertFalse(userCaptor.getValue().getIsRegistered());
    }
}
