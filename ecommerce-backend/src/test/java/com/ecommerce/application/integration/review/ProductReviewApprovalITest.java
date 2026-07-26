package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewApprovalITest extends AbstractProductReviewITest {

    @Test
    void new_review_is_hidden_until_an_admin_approves_it() throws Exception {
        Long productId = createActiveProduct("approval-flow", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 5, "Great", "Loved it");

        // Pending: invisible to the public and not in the average.
        getReviews(null, productId).andExpect(jsonPath("$.totalElements").value(0));
        getSummary(null, productId)
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.averageRating").value(0.0));

        approve(productId, reviewId);

        // Approved: now public and counted.
        getReviews(null, productId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.content[0].rating").value(5));
        getSummary(null, productId)
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));
    }

    @Test
    void admin_can_pull_the_pending_moderation_queue() throws Exception {
        Long productId = createActiveProduct("approval-queue", 10);
        postReview(userToken, productId, 5, "One", null).andExpect(status().isOk());
        String otherToken = registerAndLogin(newMobile());
        postReview(otherToken, productId, 2, "Two", null).andExpect(status().isOk());

        getReviews(adminToken, productId, Map.of("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void non_admin_status_filter_is_ignored() throws Exception {
        Long productId = createActiveProduct("approval-filter", 10);
        postReview(userToken, productId, 5, "Pending one", null).andExpect(status().isOk());

        // A public caller asking for PENDING still only ever sees PUBLISHED (here: none).
        getReviews(null, productId, Map.of("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void editing_an_approved_review_sends_it_back_to_pending() throws Exception {
        Long productId = createActiveProduct("approval-edit", 10);
        long reviewId = postAndApproveReview(userToken, productId, 5, "Great", null);
        getReviews(null, productId).andExpect(jsonPath("$.totalElements").value(1));

        updateReview(userToken, productId, reviewId, 1, "Changed my mind", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Gone from public view until re-approved.
        getReviews(null, productId).andExpect(jsonPath("$.totalElements").value(0));

        approve(productId, reviewId);
        getReviews(null, productId)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(1));
    }
}
