package com.ecommerce.application.integration.review;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The cross-product admin moderation queue ({@code GET /api/admin/reviews}).
 */
class AdminReviewQueueITest extends AbstractProductReviewITest {

    @Test
    void admin_queue_lists_all_reviews_with_product_info_and_filters_by_status() throws Exception {
        Long productA = createActiveProduct("queue-a", 10);
        Long productB = createActiveProduct("queue-b", 10);
        long pending = postReviewAndGetId(userToken, productA, 5, "Nice", null);   // stays PENDING
        postAndApproveReview(userToken, productB, 4, "Good", null);                // PUBLISHED

        // No filter → every status, each row carrying its product's name and code.
        mockMvc.perform(withAuth(get("/api/admin/reviews"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].productName").exists())
                .andExpect(jsonPath("$.content[0].productCode").exists());

        // status=PENDING → only the un-approved one.
        mockMvc.perform(withAuth(get("/api/admin/reviews").param("status", "PENDING"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) pending))
                .andExpect(jsonPath("$.content[0].productId").value(productA.intValue()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void a_normal_user_cannot_read_the_admin_queue() throws Exception {
        mockMvc.perform(withAuth(get("/api/admin/reviews"), userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void the_admin_queue_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/admin/reviews"))
                .andExpect(status().isUnauthorized());
    }
}
