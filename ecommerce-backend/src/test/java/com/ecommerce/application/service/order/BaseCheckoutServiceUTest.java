package com.ecommerce.application.service.order;

import com.ecommerce.application.config.properties.CheckoutProperties;
import com.ecommerce.application.service.address.AddressService;
import com.ecommerce.application.service.discount.DiscountService;
import com.ecommerce.application.service.shipping.ShippingCalculator;
import com.ecommerce.persistence.entity.*;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
abstract class BaseCheckoutServiceUTest {

    protected static final Long USER_ID = 7L;
    protected static final Long PRODUCT_ID = 100L;
    protected static final Long ADDRESS_ID = 55L;
    protected static final String DEFAULT_VARIANT_VALUE = "#FF0000";

    @Mock
    protected CartItemRepository cartItemRepository;
    @Mock
    protected ProductRepository productRepository;
    @Mock
    protected UserAddressRepository userAddressRepository;
    @Mock
    protected OrderRepository orderRepository;
    @Mock
    protected ShippingCalculator shippingCalculator;
    @Mock
    protected AppUserRepository appUserRepository;
    @Mock
    protected PasswordEncoder passwordEncoder;
    @Mock
    protected AddressService addressService;
    @Mock
    protected CheckoutProperties checkoutProperties;
    @Mock
    protected DiscountService discountService;

    protected CheckoutService checkoutService;

    @BeforeEach
    void baseSetUp() {
        lenient().when(checkoutProperties.getReservationTimeout()).thenReturn(Duration.ofMinutes(30));
        checkoutService = new CheckoutService(cartItemRepository, productRepository, userAddressRepository,
                orderRepository, new OrderMapperImpl(), shippingCalculator, appUserRepository, passwordEncoder,
                addressService, checkoutProperties, discountService);
        lenient().when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productRepository.decrementInventory(anyLong(), anyInt())).thenReturn(1);
    }

    protected Product product(int inventory, int weightGram, ProductStatus status) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Laptop");
        product.setCode("1-1");
        product.setStatus(status);
        product.setInventoryCount(inventory);
        product.setWeightGram(weightGram);
        product.setVariantType(VariantType.COLOR);
        Price price = new Price();
        price.setVariantValue(DEFAULT_VARIANT_VALUE);
        price.setPrice(BigDecimal.valueOf(100));
        product.setPrices(new ArrayList<>(List.of(price)));
        return product;
    }

    protected Product productWithPrice(int inventory, int weightGram, ProductStatus status,
                                        BigDecimal priceValue, BigDecimal discountPrice) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Laptop");
        product.setCode("1-1");
        product.setStatus(status);
        product.setInventoryCount(inventory);
        product.setWeightGram(weightGram);
        product.setVariantType(VariantType.COLOR);
        Price price = new Price();
        price.setVariantValue(DEFAULT_VARIANT_VALUE);
        price.setPrice(priceValue);
        price.setDiscountPrice(discountPrice);
        product.setPrices(new ArrayList<>(List.of(price)));
        return product;
    }

    protected CartItem cartItem(int quantity) {
        CartItem item = new CartItem();
        item.setId(10L);
        item.setUserId(USER_ID);
        item.setProductId(PRODUCT_ID);
        item.setVariantType(VariantType.COLOR);
        item.setVariantValue(DEFAULT_VARIANT_VALUE);
        item.setQuantity(quantity);
        return item;
    }

    protected Product variantlessProduct(int inventory, int weightGram, ProductStatus status) {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Laptop");
        product.setCode("1-1");
        product.setStatus(status);
        product.setInventoryCount(inventory);
        product.setWeightGram(weightGram);
        Price price = new Price();
        price.setPrice(BigDecimal.valueOf(100));
        product.setPrices(new ArrayList<>(List.of(price)));
        return product;
    }

    protected UserAddress address() {
        UserAddress address = new UserAddress();
        address.setId(ADDRESS_ID);
        address.setUserId(USER_ID);
        address.setRecipientFirstName("Ali");
        address.setRecipientLastName("Rezaei");
        address.setRecipientMobile("09120000000");
        address.setProvince(Province.TEHRAN);
        address.setCity("Tehran");
        address.setPostalCode("1234567890");
        address.setAddressLine("Valiasr St");
        return address;
    }
}
