package com.ecommerce.application.integration.checkout;

import com.ecommerce.application.api.dto.address.AddressRequestDto;
import com.ecommerce.application.api.dto.cart.AddCartItemRequestDto;
import com.ecommerce.application.api.dto.order.CheckoutRequestDto;
import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.PriceDto;
import com.ecommerce.application.integration.AbstractIntegrationITest;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.Province;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class AbstractCheckoutITest extends AbstractIntegrationITest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    protected String adminToken;
    protected String userToken;
    protected Long categoryId;

    @BeforeEach
    void setupCheckoutFixtures() throws Exception {
        // product / cart_item / orders / addresses are all cleared via the FK cascade chain.
        jdbcTemplate.execute("TRUNCATE TABLE category, brand RESTART IDENTITY CASCADE");

        String adminMobile = "09100000000";
        jdbcTemplate.update(
                "INSERT INTO app_user (first_name, last_name, username, mobile, password, role, is_enabled, is_registered) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "Admin", "User", adminMobile, adminMobile,
                passwordEncoder.encode("Admin123!"), "ROLE_ADMIN", true, true);
        adminToken = login(adminMobile, "Admin123!");

        userToken = registerAndLogin(newMobile());

        categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Electronics') RETURNING id", Long.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Product fixtures
    // ---------------------------------------------------------------------------------------------

    protected Long createProduct(String url, int inventory, int weightGram, ProductStatus status,
            VariantType... variants) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product " + url);
        req.setStatus(status);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(inventory);
        req.setWeightGram(weightGram);

        List<PriceDto> prices = new ArrayList<>();
        for (VariantType variant : variants) {
            PriceDto price = new PriceDto();
            price.setPrice(BigDecimal.valueOf(100));
            price.setVariantType(variant);
            prices.add(price);
        }
        req.setPrices(prices);

        org.springframework.mock.web.MockPart part =
                new org.springframework.mock.web.MockPart("data", objectMapper.writeValueAsBytes(req));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(part)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    protected Long createActiveProduct(String url, int inventory, int weightGram) throws Exception {
        return createProduct(url, inventory, weightGram, ProductStatus.ACTIVE, VariantType.COLOR);
    }

    protected Long createProductWithPrices(String url, int inventory, int weightGram,
            BigDecimal unitPrice, BigDecimal discountPrice) throws Exception {
        CreateProductRequestDto req = new CreateProductRequestDto();
        req.setCategoryId(categoryId);
        req.setUrl(url);
        req.setName("Test Product " + url);
        req.setStatus(ProductStatus.ACTIVE);
        req.setInventoryStatus(InventoryStatus.IN_STOCK);
        req.setInventoryCount(inventory);
        req.setWeightGram(weightGram);

        PriceDto price = new PriceDto();
        price.setPrice(unitPrice);
        price.setDiscountPrice(discountPrice);
        price.setVariantType(VariantType.COLOR);
        req.setPrices(List.of(price));

        org.springframework.mock.web.MockPart part =
                new org.springframework.mock.web.MockPart("data", objectMapper.writeValueAsBytes(req));
        part.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(multipart("/api/products")
                        .part(part)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    // ---------------------------------------------------------------------------------------------
    // Cart / address / checkout HTTP helpers
    // ---------------------------------------------------------------------------------------------

    protected void addToCart(String token, Long productId, VariantType variant, int quantity) throws Exception {
        AddCartItemRequestDto req = new AddCartItemRequestDto();
        req.setProductId(productId);
        req.setVariantType(variant);
        req.setQuantity(quantity);
        mockMvc.perform(withAuth(post("/api/cart/items"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    protected AddressRequestDto addressRequest(Province province) {
        AddressRequestDto dto = new AddressRequestDto();
        dto.setTitle("Home");
        dto.setRecipientFirstName("Ali");
        dto.setRecipientLastName("Rezaei");
        dto.setRecipientMobile("09120000000");
        dto.setProvince(province);
        dto.setCity("City");
        dto.setPostalCode("1234567890");
        dto.setAddressLine("Some St, No 10");
        return dto;
    }

    protected ResultActions createAddress(String token, AddressRequestDto request) throws Exception {
        return mockMvc.perform(withAuth(post("/api/addresses"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    protected long createAddressAndGetId(String token, Province province) throws Exception {
        MvcResult result = createAddress(token, addressRequest(province))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("id").asLong();
    }

    protected ResultActions listAddresses(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/addresses"), token));
    }

    protected ResultActions updateAddress(String token, long id, AddressRequestDto request) throws Exception {
        return mockMvc.perform(withAuth(put("/api/addresses/{id}", id), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    protected ResultActions deleteAddress(String token, long id) throws Exception {
        return mockMvc.perform(withAuth(delete("/api/addresses/{id}", id), token));
    }

    protected ResultActions checkout(String token, long addressId) throws Exception {
        CheckoutRequestDto req = new CheckoutRequestDto();
        req.setAddressId(addressId);
        return mockMvc.perform(withAuth(post("/api/checkout"), token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    protected ResultActions guestCheckout(Object request) throws Exception {
        return mockMvc.perform(post("/api/checkout/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    protected ResultActions listOrders(String token) throws Exception {
        return mockMvc.perform(withAuth(get("/api/orders"), token));
    }

    protected int inventoryOf(Long productId) {
        return jdbcTemplate.queryForObject(
                "SELECT inventory_count FROM product WHERE id = ?", Integer.class, productId);
    }

    protected JsonNode getCartItems(String token) throws Exception {
        MvcResult result = mockMvc.perform(withAuth(get("/api/cart"), token))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("items");
    }
}
