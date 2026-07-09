package com.ecommerce.application.service.cart;

import com.ecommerce.application.api.dto.cart.AddCartItemRequestDto;
import com.ecommerce.application.api.dto.cart.CartResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.CartItem;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.entity.enumeration.VariantValue;
import com.ecommerce.persistence.repository.CartItemRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Transactional(readOnly = true)
    public CartResponseDto getCart(Long userId) {
        return toDto(userId, cartItemRepository.findByUserId(userId));
    }

    @Transactional
    public CartResponseDto addItem(Long userId, AddCartItemRequestDto requestDto) {
        Product product = findProductOrThrow(requestDto.getProductId());
        requirePurchasable(product);

        VariantType variantType = requestDto.getVariantType();
        VariantValue variantValue = requestDto.getVariantValue();

        if (product.getVariantType() == null) {
            if (variantType != null || variantValue != null) {
                throw new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND);
            }
        } else {
            if (variantType != product.getVariantType() || variantValue == null
                    || !variantValue.isAllowedFor(product.getVariantType())) {
                throw new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND);
            }
        }
        Price price = findVariantPriceOrThrow(product, variantType, variantValue);

        CartItem item = cartItemRepository
                .findCartLine(userId, product.getId(), variantType, variantValue)
                .orElse(null);

        int newQuantity = (item != null ? item.getQuantity() : 0) + requestDto.getQuantity();
        requireSufficientStock(product, newQuantity);

        if (item == null) {
            item = new CartItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            item.setVariantType(variantType);
            item.setVariantValue(variantValue);
            item.setUnitPrice(price.getPrice());
            item.setDiscountPrice(price.getDiscountPrice());
        }
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        return getCartDto(userId);
    }

    @Transactional
    public CartResponseDto updateItemQuantity(Long userId, Long itemId, int quantity) {
        CartItem item = findItemOrThrow(userId, itemId);
        Product product = findProductOrThrow(item.getProductId());
        requireSufficientStock(product, quantity);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return getCartDto(userId);
    }

    @Transactional
    public CartResponseDto incrementItem(Long userId, Long itemId) {
        CartItem item = findItemOrThrow(userId, itemId);
        Product product = findProductOrThrow(item.getProductId());
        int newQuantity = item.getQuantity() + 1;
        requireSufficientStock(product, newQuantity);
        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
        return getCartDto(userId);
    }

    @Transactional
    public CartResponseDto decrementItem(Long userId, Long itemId) {
        CartItem item = findItemOrThrow(userId, itemId);
        if (item.getQuantity() <= 1) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(item.getQuantity() - 1);
            cartItemRepository.save(item);
        }
        return getCartDto(userId);
    }

    @Transactional
    public CartResponseDto removeItem(Long userId, Long itemId) {
        CartItem item = findItemOrThrow(userId, itemId);
        cartItemRepository.delete(item);
        return getCartDto(userId);
    }

    @Transactional
    public CartResponseDto clearCart(Long userId) {
        // Clearing is idempotent: with no rows for the user this simply deletes nothing.
        cartItemRepository.deleteByUserId(userId);
        return toDto(userId, List.of());
    }

    private CartItem findItemOrThrow(Long userId, Long itemId) {
        return cartItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.CART_ITEM_NOT_FOUND));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND));
    }

    private void requirePurchasable(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_NOT_AVAILABLE);
        }
    }

    /**
     * Picks the Price for the line. A variant-less product (both args null) gets the first row
     * whose own variantValue is null; a variant-bearing request gets the row matching exactly.
     * Throws {@code PRODUCT_VARIANT_NOT_FOUND} if no matching price exists.
     */
    private Price findVariantPriceOrThrow(Product product, VariantType variantType, VariantValue variantValue) {
        if (product.getVariantType() == null) {
            if (variantType != null || variantValue != null) {
                throw new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND);
            }
            return product.getPrices().stream()
                    .filter(p -> p.getVariantValue() == null)
                    .findFirst()
                    .or(() -> product.getPrices().stream().findFirst())
                    .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND));
        }
        if (product.getVariantType() != variantType) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND);
        }
        return product.getPrices().stream()
                .filter(p -> variantValue.equals(p.getVariantValue()))
                .findFirst()
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_VARIANT_NOT_FOUND));
    }

    private void requireSufficientStock(Product product, int requestedQuantity) {
        if (product.getInventoryCount() == null || requestedQuantity > product.getInventoryCount()) {
            throw new EcommerceException(ECOMErrorType.INSUFFICIENT_STOCK);
        }
    }

    private CartResponseDto getCartDto(Long userId) {
        return toDto(userId, cartItemRepository.findByUserId(userId));
    }

    private CartResponseDto toDto(Long userId, List<CartItem> items) {
        List<Long> productIds = items.stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));
        return cartMapper.toResponseDto(userId, items, products);
    }
}