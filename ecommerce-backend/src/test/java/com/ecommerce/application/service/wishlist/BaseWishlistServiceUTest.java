package com.ecommerce.application.service.wishlist;

import com.ecommerce.application.api.dto.wishlist.AddWishlistItemRequestDto;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.ProductImage;
import com.ecommerce.persistence.entity.WishlistItem;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.WishlistItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
abstract class BaseWishlistServiceUTest {

    protected static final Long USER_ID = 7L;
    protected static final Long PRODUCT_ID = 100L;

    @Mock
    protected WishlistItemRepository wishlistItemRepository;
    @Mock
    protected ProductRepository productRepository;

    protected WishlistService wishlistService;

    @BeforeEach
    void baseSetUp() {
        wishlistService = new WishlistService(wishlistItemRepository, productRepository, new WishlistMapperImpl());
        // The insert reports one row created; tests covering the already-bookmarked path override this.
        lenient().when(wishlistItemRepository.insertIfAbsent(any(), any())).thenReturn(1);
    }

    protected void stubProductsForDto(Product... products) {
        lenient().when(productRepository.findAllById(anyIterable())).thenReturn(List.of(products));
    }

    /** Stubs the rendered wishlist (the read-back after a mutation, and getWishlist itself). */
    protected void stubUserItems(WishlistItem... items) {
        when(wishlistItemRepository.findByUserIdOrderByCreatedAtDescIdDesc(USER_ID))
                .thenReturn(new ArrayList<>(List.of(items)));
    }

    protected Product product(Long id, ProductStatus status, int inventory) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setLocalName("محصول " + id);
        product.setCode(id + "-1");
        product.setUrl("product-" + id);
        product.setStatus(status);
        product.setInventoryStatus(inventory > 0 ? InventoryStatus.IN_STOCK : InventoryStatus.OUT_OF_STOCK);
        product.setInventoryCount(inventory);

        ProductImage image = new ProductImage();
        image.setAltText("alt " + id);
        image.setImageData("data" + id);
        product.setMainImage(image);

        product.setVariantType(VariantType.COLOR);
        Price price = new Price();
        price.setVariantValue("#FF0000");
        price.setPrice(BigDecimal.valueOf(100));
        product.setPrices(new ArrayList<>(List.of(price)));
        return product;
    }

    protected Product activeProduct(Long id) {
        return product(id, ProductStatus.ACTIVE, 10);
    }

    protected WishlistItem item(Long id, Long productId) {
        WishlistItem item = new WishlistItem();
        item.setId(id);
        item.setUserId(USER_ID);
        item.setProductId(productId);
        item.setCreatedAt(new Date());
        return item;
    }

    protected AddWishlistItemRequestDto addRequest(Long productId) {
        AddWishlistItemRequestDto requestDto = new AddWishlistItemRequestDto();
        requestDto.setProductId(productId);
        return requestDto;
    }
}
