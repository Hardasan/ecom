package com.ecommerce.application.controller;

import com.ecommerce.application.api.dto.admin.AdminStatsResponseDto;
import com.ecommerce.application.api.dto.review.AdminReviewResponseDto;
import com.ecommerce.application.service.admin.AdminStatsService;
import com.ecommerce.application.service.review.ProductReviewService;
import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cross-cutting admin reads that do not belong under a single resource: the dashboard summary and
 * the global (all-products) review moderation queue. Per-product review moderation still lives on
 * {@link ProductReviewController}.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminStatsService adminStatsService;
    private final ProductReviewService productReviewService;

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public AdminStatsResponseDto stats() {
        return adminStatsService.getStats();
    }

    @GetMapping(value = "/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdminReviewResponseDto> reviews(
            @RequestParam(required = false) ReviewStatus status, Pageable pageable) {
        return productReviewService.getModerationQueue(status, pageable);
    }
}
