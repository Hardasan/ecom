package com.ecommerce.application.integration.review;

import com.ecommerce.persistence.entity.enumeration.ReviewStatus;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewModerationITest extends AbstractProductReviewITest {

    @Test
    void admin_hiding_a_review_removes_it_from_public_view_and_the_summary() throws Exception {
        Long productId = createActiveProduct("moderate-hide", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 5, "Great", null);

        moderate(adminToken, productId, reviewId, ReviewStatus.HIDDEN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));

        // Hidden from the public list and excluded from the average...
        getReviews(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        getSummary(null, productId)
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));

        // ...but still visible to an admin, flagged HIDDEN.
        getReviews(adminToken, productId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"));
    }

    @Test
    void admin_can_unhide_a_review() throws Exception {
        Long productId = createActiveProduct("moderate-unhide", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 4, "Fine", null);
        moderate(adminToken, productId, reviewId, ReviewStatus.HIDDEN).andExpect(status().isOk());

        moderate(adminToken, productId, reviewId, ReviewStatus.PUBLISHED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        getReviews(null, productId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(4));
    }

    @Test
    void a_normal_user_cannot_moderate_and_gets_403() throws Exception {
        Long productId = createActiveProduct("moderate-forbidden", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 5, "Great", null);

        moderate(userToken, productId, reviewId, ReviewStatus.HIDDEN)
                .andExpect(status().isForbidden());
    }

    @Test
    void moderating_without_auth_returns_401() throws Exception {
        Long productId = createActiveProduct("moderate-noauth", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 5, "Great", null);

        moderate(null, productId, reviewId, ReviewStatus.HIDDEN)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void moderating_an_unknown_review_returns_404() throws Exception {
        Long productId = createActiveProduct("moderate-unknown", 10);
        moderate(adminToken, productId, 999_999L, ReviewStatus.HIDDEN)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_NOT_FOUND"));
    }
}
