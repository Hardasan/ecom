package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductReviewListITest extends AbstractProductReviewITest {

    @Test
    void list_is_public_and_empty_for_a_product_with_no_reviews() throws Exception {
        Long productId = createActiveProduct("empty", 10);
        getReviews(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void reviews_are_isolated_between_products() throws Exception {
        Long productA = createActiveProduct("prod-a", 10);
        Long productB = createActiveProduct("prod-b", 10);
        postReview(userToken, productA, 5, "A review", null).andExpect(status().isOk());

        getReviews(null, productB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void newest_is_the_default_order() throws Exception {
        Long productId = createActiveProduct("sort-newest", 10);
        seedTwoReviews(productId);

        getReviews(null, productId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].rating").value(3)); // the later-posted review first
    }

    @Test
    void oldest_sort_returns_the_first_posted_first() throws Exception {
        Long productId = createActiveProduct("sort-oldest", 10);
        seedTwoReviews(productId);

        getReviews(null, productId, Map.of("sort", "OLDEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void highest_sort_returns_the_best_rating_first() throws Exception {
        Long productId = createActiveProduct("sort-highest", 10);
        seedTwoReviews(productId);

        getReviews(null, productId, Map.of("sort", "HIGHEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void lowest_sort_returns_the_worst_rating_first() throws Exception {
        Long productId = createActiveProduct("sort-lowest", 10);
        seedTwoReviews(productId);

        getReviews(null, productId, Map.of("sort", "LOWEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(3));
    }

    @Test
    void filter_by_rating_returns_only_that_star() throws Exception {
        Long productId = createActiveProduct("filter-rating", 10);
        seedTwoReviews(productId);

        getReviews(null, productId, Map.of("rating", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void filter_verified_only_returns_only_verified_reviews() throws Exception {
        Long productId = createActiveProduct("filter-verified", 10);
        // userToken bought the product -> verified; a second user did not.
        insertPaidOrder(userId, productId);
        postReview(userToken, productId, 5, "Verified buyer", null).andExpect(status().isOk());
        String otherToken = registerAndLogin(newMobile());
        postReview(otherToken, productId, 2, "Not a buyer", null).andExpect(status().isOk());

        getReviews(null, productId, Map.of("verifiedOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.content[0].verifiedPurchase").value(true));
    }

    @Test
    void listing_reviews_of_unknown_product_returns_404() throws Exception {
        getReviews(null, 999_999L)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"));
    }

    /** userToken posts a 5-star review, then a second fresh user posts a 3-star review. */
    private void seedTwoReviews(Long productId) throws Exception {
        postReview(userToken, productId, 5, "First and best", null).andExpect(status().isOk());
        String otherToken = registerAndLogin(newMobile());
        postReview(otherToken, productId, 3, "Second and meh", null).andExpect(status().isOk());
    }
}
