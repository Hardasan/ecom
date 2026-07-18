package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewCreateITest extends AbstractProductReviewITest {

    @Test
    void post_without_auth_returns_401() throws Exception {
        Long productId = createActiveProduct("no-auth", 10);
        postReview(null, productId, 5, "Nice", "Good product")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void post_creates_review_that_appears_in_the_public_list() throws Exception {
        Long productId = createActiveProduct("create-1", 10);

        postReview(userToken, productId, 5, "Excellent", "Would buy again")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.title").value("Excellent"))
                .andExpect(jsonPath("$.comment").value("Would buy again"))
                .andExpect(jsonPath("$.authorName").value("Test User"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.verifiedPurchase").value(false));

        getReviews(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rating").value(5));

        assertReviewRows(productId, 1);
    }

    @Test
    void rating_only_without_text_is_allowed() throws Exception {
        Long productId = createActiveProduct("rating-only", 10);
        postReview(userToken, productId, 4, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.comment").doesNotExist());
    }

    @Test
    void second_review_by_same_user_returns_409() throws Exception {
        Long productId = createActiveProduct("dup", 10);
        postReview(userToken, productId, 5, "First", null).andExpect(status().isOk());

        postReview(userToken, productId, 3, "Second", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_REVIEW_ALREADY_EXISTS"));

        assertReviewRows(productId, 1);
    }

    @Test
    void rating_below_one_is_rejected() throws Exception {
        Long productId = createActiveProduct("rating-low", 10);
        postReview(userToken, productId, 0, "bad", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void rating_above_five_is_rejected() throws Exception {
        Long productId = createActiveProduct("rating-high", 10);
        postReview(userToken, productId, 6, "too good", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void missing_rating_is_rejected() throws Exception {
        Long productId = createActiveProduct("rating-missing", 10);
        postReview(userToken, productId, null, "no rating", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void reviewing_unknown_product_returns_404() throws Exception {
        postReview(userToken, 999_999L, 5, "ghost", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void verified_purchase_is_true_after_a_paid_order() throws Exception {
        Long productId = createActiveProduct("verified", 10);
        insertPaidOrder(userId, productId);

        postReview(userToken, productId, 5, "Bought and loved it", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedPurchase").value(true));
    }

    @Test
    void verified_purchase_is_false_without_a_paid_order() throws Exception {
        Long productId = createActiveProduct("unverified", 10);

        postReview(userToken, productId, 5, "Just browsing", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedPurchase").value(false));
    }

    private void assertReviewRows(Long productId, long expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, reviewRowCount(productId));
    }
}
