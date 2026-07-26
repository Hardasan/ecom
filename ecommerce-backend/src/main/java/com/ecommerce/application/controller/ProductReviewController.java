package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.review.ModerateReviewRequestDto;
import com.ecommerce.application.api.dto.review.ReviewRequestDto;
import com.ecommerce.application.api.dto.review.ReviewResponseDto;
import com.ecommerce.application.api.dto.review.ReviewSummaryResponseDto;
import com.ecommerce.application.api.dto.review.SearchReviewRequestDto;
import com.ecommerce.application.config.security.UserDetailsDto;
import com.ecommerce.application.service.review.ProductReviewService;
import com.ecommerce.application.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService productReviewService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<ReviewResponseDto> list(@PathVariable Long productId,
            @ModelAttribute SearchReviewRequestDto searchDto, Pageable pageable) {
        return productReviewService.getReviews(productId, SecurityUtil.isAdmin(), searchDto, pageable);
    }

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReviewSummaryResponseDto summary(@PathVariable Long productId) {
        return productReviewService.getSummary(productId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ReviewResponseDto create(@PathVariable Long productId,
            @RequestBody ReviewRequestDto requestDto, Authentication authentication) {
        return productReviewService.create(userId(authentication), productId, requestDto);
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ReviewResponseDto update(@PathVariable Long productId, @PathVariable Long reviewId,
            @RequestBody ReviewRequestDto requestDto, Authentication authentication) {
        return productReviewService.update(userId(authentication), productId, reviewId, requestDto);
    }

    @DeleteMapping("/{reviewId}")
    public void delete(@PathVariable Long productId, @PathVariable Long reviewId, Authentication authentication) {
        productReviewService.delete(userId(authentication), SecurityUtil.isAdmin(), productId, reviewId);
    }

    @PatchMapping(value = "/{reviewId}/status", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewResponseDto moderate(@PathVariable Long productId, @PathVariable Long reviewId,
            @RequestBody ModerateReviewRequestDto requestDto) {
        return productReviewService.moderate(productId, reviewId, requestDto.getStatus());
    }

    private Long userId(Authentication authentication) {
        return ((UserDetailsDto) authentication.getPrincipal()).getId();
    }
}
