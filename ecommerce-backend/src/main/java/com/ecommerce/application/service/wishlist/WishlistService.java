package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.AddWishlistItemRequestDto;
import com.ecommerce.application.api.dto.wishlist.WishlistContainsResponseDto;
import com.ecommerce.application.api.dto.wishlist.WishlistResponseDto;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.WishlistItem;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    @Transactional(readOnly = true)
    public WishlistResponseDto getWishlist(Long userId) {
        return toDto(userId, wishlistItemRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId));
    }

    @Transactional
    public WishlistResponseDto addItem(Long userId, AddWishlistItemRequestDto requestDto) {
        Product product = findProductOrThrow(requestDto.getProductId());
        requireWishlistable(product);

        // Adding a product already on the wishlist is a no-op: the wishlist holds at most one
        // bookmark per (user, product), so a second add neither errors nor duplicates the row.
        if (!wishlistItemRepository.existsByUserIdAndProductId(userId, product.getId())) {
            WishlistItem item = new WishlistItem();
            item.setUserId(userId);
            item.setProductId(product.getId());
            wishlistItemRepository.save(item);
        }

        return getWishlistDto(userId);
    }

    @Transactional
    public WishlistResponseDto removeItem(Long userId, Long itemId) {
        WishlistItem item = wishlistItemRepository.findByIdAndUserId(itemId, userId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.WISHLIST_ITEM_NOT_FOUND));
        wishlistItemRepository.delete(item);
        return getWishlistDto(userId);
    }

    @Transactional
    public WishlistResponseDto removeByProduct(Long userId, Long productId) {
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.WISHLIST_ITEM_NOT_FOUND));
        wishlistItemRepository.delete(item);
        return getWishlistDto(userId);
    }

    @Transactional
    public WishlistResponseDto clear(Long userId) {
        // Clearing is idempotent: with no rows for the user this simply deletes nothing.
        wishlistItemRepository.deleteByUserId(userId);
        return toDto(userId, List.of());
    }

    @Transactional(readOnly = true)
    public WishlistContainsResponseDto contains(Long userId, Long productId) {
        return new WishlistContainsResponseDto(
                wishlistItemRepository.existsByUserIdAndProductId(userId, productId));
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND));
    }

    private void requireWishlistable(Product product) {
        // Unlike the cart, stock is deliberately NOT checked here: saving an out-of-stock product to
        // buy once it returns is a primary reason to keep a wishlist. Only fully delisted (non-ACTIVE)
        // products are rejected, since they are not part of the shoppable catalogue at all.
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_NOT_AVAILABLE);
        }
    }

    private WishlistResponseDto getWishlistDto(Long userId) {
        return toDto(userId, wishlistItemRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId));
    }

    private WishlistResponseDto toDto(Long userId, List<WishlistItem> items) {
        List<Long> productIds = items.stream()
                .map(WishlistItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> products = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));
        return wishlistMapper.toResponseDto(userId, items, products);
    }
}
