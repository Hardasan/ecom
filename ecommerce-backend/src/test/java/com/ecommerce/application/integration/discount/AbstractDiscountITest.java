package com.ecommerce.application.integration.discount;

import com.ecommerce.application.api.dto.discount.CreateDiscountRequestDto;
import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.checkout.AbstractCheckoutITest;
import com.ecommerce.application.service.order.ReservationReleaseService;
import com.ecommerce.persistence.entity.enumeration.DiscountScope;
import com.ecommerce.persistence.entity.enumeration.DiscountType;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared fixtures for the discount ITests. Reuses the checkout base (admin/user tokens, category,
 * product/cart/address/checkout helpers) and additionally clears the discount tables per test.
 */
public abstract class AbstractDiscountITest extends AbstractCheckoutITest {

    @Autowired
    protected ReservationReleaseService reservationReleaseService;
    @Autowired
    protected PasswordEncoder discountPasswordEncoder;

    @BeforeEach
    void setupDiscountFixtures() {
        // Cascades to discount_product / discount_category (and to orders, already empty from the
        // app_user truncate). discount_seq is a standalone sequence, so ids keep climbing — tests
        // never assume a specific id.
        jdbcTemplate.execute("TRUNCATE TABLE discount RESTART IDENTITY CASCADE");
    }

    // ---------------------------------------------------------------------------------------------
    // Discount request builders
    // ---------------------------------------------------------------------------------------------

    protected CreateDiscountRequestDto percentage(String code, long pct) {
        return discount(code, DiscountType.PERCENTAGE, BigDecimal.valueOf(pct), DiscountScope.ALL);
    }

    protected CreateDiscountRequestDto fixed(String code, long amount) {
        return discount(code, DiscountType.FIXED_AMOUNT, BigDecimal.valueOf(amount), DiscountScope.ALL);
    }

    protected CreateDiscountRequestDto discount(String code, DiscountType type, BigDecimal value,
            DiscountScope scope) {
        CreateDiscountRequestDto dto = new CreateDiscountRequestDto();
        dto.setCode(code);
        dto.setType(type);
        dto.setValue(value);
        dto.setScope(scope);
        return dto;
    }

    // ---------------------------------------------------------------------------------------------
    // Discount HTTP helpers
    // ---------------------------------------------------------------------------------------------

    protected ResultActions createDiscountRequest(CreateDiscountRequestDto dto, String token) throws Exception {
        return mockMvc.perform(withAuth(post("/api/discounts"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    protected long createDiscount(CreateDiscountRequestDto dto) throws Exception {
        MvcResult result = createDiscountRequest(dto, adminToken)
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    protected ResultActions preview(String token, String code) throws Exception {
        return mockMvc.perform(withAuth(post("/api/discounts/preview"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("code", code))));
    }

    protected ResultActions checkoutWithDiscount(String token, long addressId, String code) throws Exception {
        CheckoutRequestDto req = new CheckoutRequestDto();
        req.setAddressId(addressId);
        req.setDiscountCode(code);
        return mockMvc.perform(withAuth(post("/api/checkout"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    // ---------------------------------------------------------------------------------------------
    // Catalog helpers (a product in an arbitrary category, for scope tests)
    // ---------------------------------------------------------------------------------------------

    protected Long createCategory(String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES (?) RETURNING id", Long.class, name);
    }

    protected Long createProductInCategory(String url, Long catId, int inventory, int weightGram,
            BigDecimal price) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(catId);
        req.setUrl(url);
        req.setName("Test Product " + url);
        req.setStatus(ProductStatus.ACTIVE);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(inventory);
        req.setWeightGram(weightGram);
        req.setVariantType(DEFAULT_VARIANT_TYPE);
        PriceDto priceDto = new PriceDto();
        priceDto.setPrice(price);
        priceDto.setVariantValue(DEFAULT_VARIANT_VALUE);
        req.setPrices(List.of(priceDto));

        MockPart part = new MockPart("data", objectMapper.writeValueAsBytes(req));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(part)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    /**
     * Inserts an enabled/registered user directly (fast path for the concurrency test) and logs in.
     */
    protected String createUserAndLogin(String mobile) throws Exception {
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Load", "User", mobile, mobile,
                discountPasswordEncoder.encode(DEFAULT_PASSWORD), "ROLE_APP_USER", true, true);
        return login(mobile, DEFAULT_PASSWORD);
    }

    // ---------------------------------------------------------------------------------------------
    // DB assertions
    // ---------------------------------------------------------------------------------------------

    protected int usageCount(long discountId) {
        return jdbcTemplate.queryForObject(
                "SELECT usage_count FROM discount WHERE id = ?", Integer.class, discountId);
    }

    protected long orderCountForDiscount(long discountId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE discount_id = ?", Long.class, discountId);
    }

    /**
     * Asserts discountAmount and the totalCost = itemsCost - discount + shippingCost invariant.
     */
    protected void assertDiscountApplied(JsonNode order, String code, BigDecimal expectedDiscount) {
        assertEquals(code, order.get("discountCode").asText());
        assertEquals(0, expectedDiscount.compareTo(order.get("discountAmount").decimalValue()),
                () -> "discountAmount was " + order.get("discountAmount"));
        BigDecimal items = order.get("itemsCost").decimalValue();
        BigDecimal shipping = order.get("shippingCost").decimalValue();
        BigDecimal total = order.get("totalCost").decimalValue();
        assertEquals(0, items.subtract(expectedDiscount).add(shipping).compareTo(total),
                () -> "total " + total + " != items " + items + " - discount " + expectedDiscount + " + shipping " + shipping);
    }
}
