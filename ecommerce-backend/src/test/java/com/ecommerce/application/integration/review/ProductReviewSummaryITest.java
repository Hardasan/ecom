package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewSummaryITest extends AbstractProductReviewITest {

    @Test
    void summary_is_public_and_zeroed_for_a_product_with_no_reviews() throws Exception {
        Long productId = createActiveProduct("summary-empty", 10);

        getSummary(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.ratingCounts['1']").value(0))
                .andExpect(jsonPath("$.ratingCounts['5']").value(0));
    }

    @Test
    void summary_reports_average_total_and_distribution() throws Exception {
        Long productId = createActiveProduct("summary-mix", 10);
        postFreshUser(productId, 5);
        postFreshUser(productId, 4);
        postFreshUser(productId, 3);

        getSummary(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.ratingCounts['5']").value(1))
                .andExpect(jsonPath("$.ratingCounts['4']").value(1))
                .andExpect(jsonPath("$.ratingCounts['3']").value(1))
                .andExpect(jsonPath("$.ratingCounts['2']").value(0))
                .andExpect(jsonPath("$.ratingCounts['1']").value(0));
    }

    @Test
    void summary_rounds_the_average_half_up_to_one_decimal() throws Exception {
        Long productId = createActiveProduct("summary-round", 10);
        // 5 + 4 + 4 = 13 / 3 = 4.333... -> 4.3
        postFreshUser(productId, 5);
        postFreshUser(productId, 4);
        postFreshUser(productId, 4);

        getSummary(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(3))
                .andExpect(jsonPath("$.averageRating").value(4.3));
    }

    private void postFreshUser(Long productId, int rating) throws Exception {
        String token = registerAndLogin(newMobile());
        postAndApproveReview(token, productId, rating, null, null);
    }
}
