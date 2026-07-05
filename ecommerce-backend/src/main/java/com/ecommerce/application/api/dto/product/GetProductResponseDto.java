package com.ecommerce.application.api.dto.product;

import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.SpecificationKey;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GetProductResponseDto {

    private Long id;

    private String code;

    private Long categoryId;

    private Long subCategoryId;

    private String url;

    private List<PriceDto> prices;

    private String shortDescription;

    private String fullDescription;

    private Map<SpecificationKey, String> specification;

    private ProductImageDto mainImage;

    private List<ProductOtherImageDto> otherImages;

    private String name;

    private String localName;

    private Long brandId;

    private InventoryStatus inventoryStatus;

    private ProductStatus status;

    private Integer inventoryCount;

    private Integer weightGram;

    private Date createdAt;

    private Date updatedAt;
}
