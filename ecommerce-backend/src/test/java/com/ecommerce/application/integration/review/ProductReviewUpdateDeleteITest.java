package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewUpdateDeleteITest extends AbstractProductReviewITest {

    @Test
    void owner_updates_own_review() throws Exception {
        Long productId = createActiveProduct("update-own", 10);
        long reviewId = postAndApproveReview(userToken, productId, 3, "Ok", "It was fine");

        updateReview(userToken, productId, reviewId, 5, "Actually great", "Grew on me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Actually great"))
                .andExpect(jsonPath("$.comment").value("Grew on me"))
                .andExpect(jsonPath("$.status").value("PENDING")); // an edit needs re-approval

        // The edit pulls it back out of the public list until re-approved.
        getReviews(null, productId).andExpect(jsonPath("$.totalElements").value(0));
        getReviews(adminToken, productId)
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void update_without_auth_returns_401() throws Exception {
        Long productId = createActiveProduct("update-noauth", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 3, "Ok", null);

        updateReview(null, productId, reviewId, 5, "hack", null)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updating_another_users_review_returns_404() throws Exception {
        Long productId = createActiveProduct("update-foreign", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 3, "Mine", null);

        String otherToken = registerAndLogin(newMobile());
        updateReview(otherToken, productId, reviewId, 1, "Not mine", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_NOT_FOUND"));
    }

    @Test
    void updating_a_review_under_the_wrong_product_returns_404() throws Exception {
        Long productA = createActiveProduct("update-prod-a", 10);
        Long productB = createActiveProduct("update-prod-b", 10);
        long reviewId = postReviewAndGetId(userToken, productA, 4, "On A", null);

        updateReview(userToken, productB, reviewId, 1, "Wrong product", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_NOT_FOUND"));
    }

    @Test
    void owner_deletes_own_review() throws Exception {
        Long productId = createActiveProduct("delete-own", 10);
        long reviewId = postAndApproveReview(userToken, productId, 4, "Bye", null);

        deleteReview(userToken, productId, reviewId).andExpect(status().isOk());

        getReviews(null, productId).andExpect(jsonPath("$.totalElements").value(0));
        assertEquals(0L, reviewRowCount(productId));
    }

    @Test
    void deleting_another_users_review_as_a_normal_user_returns_404() throws Exception {
        Long productId = createActiveProduct("delete-foreign", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 4, "Mine", null);

        String otherToken = registerAndLogin(newMobile());
        deleteReview(otherToken, productId, reviewId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_NOT_FOUND"));
        assertEquals(1L, reviewRowCount(productId));
    }

    @Test
    void admin_can_delete_any_review() throws Exception {
        Long productId = createActiveProduct("delete-admin", 10);
        long reviewId = postReviewAndGetId(userToken, productId, 4, "User review", null);

        deleteReview(adminToken, productId, reviewId).andExpect(status().isOk());
        assertEquals(0L, reviewRowCount(productId));
    }

    @Test
    void deleting_unknown_review_returns_404() throws Exception {
        Long productId = createActiveProduct("delete-unknown", 10);
        deleteReview(userToken, productId, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_NOT_FOUND"));
    }
}
