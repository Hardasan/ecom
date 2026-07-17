package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.AddCartItemRequestDto;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.CartItemRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
abstract class BaseCartServiceUTest {

    protected static final Long USER_ID = 7L;
    protected static final Long PRODUCT_ID = 100L;
    protected static final String DEFAULT_VARIANT_VALUE = "RED";

    @Mock
    protected CartItemRepository cartItemRepository;
    @Mock
    protected ProductRepository productRepository;

    protected CartService cartService;

    @BeforeEach
    void baseSetUp() {
        cartService = new CartService(cartItemRepository, productRepository, new CartMapperImpl());
        // save returns the persisted instance; harmless if a given test never saves.
        lenient().when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    protected void stubProductsForDto(Product... products) {
        lenient().when(productRepository.findAllById(anyIterable())).thenReturn(List.of(products));
    }

    /** Stubs the rendered cart (the read-back after a mutation, and getCart itself). */
    protected void stubUserItems(CartItem... items) {
        when(cartItemRepository.findByUserId(USER_ID)).thenReturn(new ArrayList<>(List.of(items)));
    }

    protected Product product(Long id, int inventory, ProductStatus status, VariantType variant, String variantValue,
                              BigDecimal price, BigDecimal discountPrice) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setCode(id + "-1");
        product.setStatus(status);
        product.setInventoryCount(inventory);
        product.setVariantType(variant);
        Price variantPrice = new Price();
        variantPrice.setVariantValue(variantValue);
        variantPrice.setPrice(price);
        variantPrice.setDiscountPrice(discountPrice);
        product.setPrices(new ArrayList<>(List.of(variantPrice)));
        return product;
    }

    protected Product product(Long id, int inventory) {
        return product(id, inventory, ProductStatus.ACTIVE, VariantType.COLOR, DEFAULT_VARIANT_VALUE,
                BigDecimal.valueOf(100), null);
    }

    protected CartItem item(Long id, Long productId, VariantType variant, String variantValue, int quantity,
                            BigDecimal unitPrice, BigDecimal discountPrice) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(USER_ID);
        item.setProductId(productId);
        item.setVariantType(variant);
        item.setVariantValue(variantValue);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setDiscountPrice(discountPrice);
        return item;
    }

    protected AddCartItemRequestDto addRequest(Long productId, VariantType variant, String variantValue, int quantity) {
        AddCartItemRequestDto requestDto = new AddCartItemRequestDto();
        requestDto.setProductId(productId);
        requestDto.setVariantType(variant);
        requestDto.setVariantValue(variantValue == null ? null : variantValue);
        requestDto.setQuantity(quantity);
        return requestDto;
    }

    protected AddCartItemRequestDto addRequestVariantless(Long productId, int quantity) {
        AddCartItemRequestDto requestDto = new AddCartItemRequestDto();
        requestDto.setProductId(productId);
        requestDto.setQuantity(quantity);
        return requestDto;
    }

    protected Product variantlessProduct(Long id, int inventory, ProductStatus status,
                                         BigDecimal price, BigDecimal discountPrice) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setCode(id + "-1");
        product.setStatus(status);
        product.setInventoryCount(inventory);
        Price variantPrice = new Price();
        variantPrice.setPrice(price);
        variantPrice.setDiscountPrice(discountPrice);
        product.setPrices(new ArrayList<>(List.of(variantPrice)));
        return product;
    }

    protected CartItem variantlessItem(Long id, Long productId, int quantity,
                                       BigDecimal unitPrice, BigDecimal discountPrice) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setUserId(USER_ID);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setDiscountPrice(discountPrice);
        return item;
    }
}