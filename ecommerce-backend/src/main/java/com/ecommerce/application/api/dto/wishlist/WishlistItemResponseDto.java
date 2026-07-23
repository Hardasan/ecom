package com.ecommerce.application.api.dto.wishlist;

import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.api.dto.product.ProductImageDto;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class WishlistItemResponseDto {

    private Long id;

    private Long productId;

    private String productName;

    private String productLocalName;

    private String productCode;

    private String productUrl;

    private ProductImageDto mainImage;

    private List<PriceDto> prices;

    private ProductStatus status;

    private InventoryStatus inventoryStatus;

    private Integer inventoryCount;

    // Derived catalog flags so a wishlist page can badge each line without a second lookup:
    // inStock = there is inventory; available = additionally purchasable right now (ACTIVE + in stock).
    private Boolean inStock;

    private Boolean available;

    private Date addedAt;
}
