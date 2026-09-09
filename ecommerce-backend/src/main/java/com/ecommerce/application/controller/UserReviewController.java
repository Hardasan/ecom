package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.review.AdminReviewResponseDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.review.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in shopper's own reviews («نظرات من» in the profile). Product review reads/writes are
 * product-scoped under {@link ProductReviewController}; this cross-product "my reviews" list is
 * keyed off the authenticated user, so it lives on its own authenticated path (not public).
 */
@RestController
@RequestMapping("/api/user/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<AdminReviewResponseDto> myReviews(Authentication authentication, Pageable pageable) {
        Long userId = ((UserDetailsDto) authentication.getPrincipal()).getId();
        return productReviewService.getMyReviews(userId, pageable);
    }
}
