package com.ecommerce.application.service.review;

import com.ecommerce.application.api.dto.review.ReviewRequestDto;
import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.persistence.entity.ProductReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductReviewMapper {

    // Only the client-editable fields (rating/title/comment) are applied; everything else is
    // server-managed and set explicitly in the service.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "authorName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "verifiedPurchase", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(ReviewRequestDto dto, @MappingTarget ProductReview entity);

    ReviewResponseDto toResponseDto(ProductReview entity);
}
