package com.ecommerce.application.integration.product;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductBatchUploadITest extends AbstractProductITest {

    @Test
    void download_template_returns_valid_excel() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products/template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("product-template.xlsx")))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertTrue(content.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Products");
            assertNotNull(sheet);
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("Name", headerRow.getCell(0).getStringCellValue());
            assertEquals("URL", headerRow.getCell(2).getStringCellValue());
            assertEquals("Category", headerRow.getCell(3).getStringCellValue());
        }
    }

    @Test
    void download_template_requires_admin() throws Exception {
        mockMvc.perform(get("/api/products/template")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_valid_excel_saves_products_in_database() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("DB Product", "محصول دیتابیس", "db-product-url", "Electronics", "", "",
                        "Desc", "199000", "", "COLOR", "#FF0000", "15", "200", "ACTIVE", "IN_STOCK")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        // Verify all columns
        var row = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "db-product-url");
        assertEquals("DB Product", row.get("name"));
        assertEquals("محصول دیتابیس", row.get("local_name"));
        assertEquals("db-product-url", row.get("url"));
        assertEquals(categoryId, row.get("category_id"));
        assertEquals("Desc", row.get("short_description"));
        assertNull(row.get("full_description"));
        assertEquals(15, row.get("inventory_count"));
        assertEquals(200, row.get("weight_gram"));
        assertEquals("ACTIVE", row.get("status"));
        assertEquals("IN_STOCK", row.get("inventory_status"));
        assertEquals("COLOR", row.get("variant_type"));
        assertNotNull(row.get("created_at"));
        assertNotNull(row.get("updated_at"));
        // code format: {categoryId}-{seq}
        String code = (String) row.get("code");
        assertTrue(code.matches(categoryId + "-\\d+"), "Expected code like " + categoryId + "-N, got: " + code);

        // Verify price in product_price table
        var priceRow = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", row.get("id"));
        assertEquals(0, new java.math.BigDecimal("1990000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertNull(priceRow.get("discount_price"));
        assertEquals("#FF0000", priceRow.get("variant_value"));
    }

    @Test
    void upload_multiple_products_all_saved() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Multi 1", "", "multi-1", "Electronics", "", "", "D1", "100000", "", "COLOR", "#FF0000", "5", "100",
                        "ACTIVE", "IN_STOCK"),
                row("Multi 2", "", "multi-2", "Electronics", "", "", "D2", "200000", "180000", "SIZE", "M", "8", "150",
                        "ACTIVE", "IN_STOCK"),
                row("Multi 3", "", "multi-3", "Electronics", "", "", "D3", "300000", "", "SIZE", "XL", "12", "200",
                        "ACTIVE", "IN_STOCK")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(3, json.get("successCount").asInt(), json.toString());

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE url IN ('multi-1','multi-2','multi-3')", Integer.class);
        assertEquals(3, count);

        // Verify Multi 2 has discount price and SIZE variant
        var multi2 = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "multi-2");
        assertEquals("Multi 2", multi2.get("name"));
        assertEquals(8, multi2.get("inventory_count"));
        assertEquals(150, multi2.get("weight_gram"));
        assertEquals("SIZE", multi2.get("variant_type"));

        var price2 = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", multi2.get("id"));
        assertEquals(0, new java.math.BigDecimal("2000000")
                .compareTo((java.math.BigDecimal) price2.get("price")));
        assertEquals(0, new java.math.BigDecimal("1800000")
                .compareTo((java.math.BigDecimal) price2.get("discount_price")));
        assertEquals("M", price2.get("variant_value"));

        // Verify Multi 1 has null local_name (empty in Excel → null in DB)
        var multi1 = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "multi-1");
        assertNull(multi1.get("local_name"));
    }

    @Test
    void upload_with_missing_required_fields_returns_errors() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Incomplete", "", "", "", "", "", "", "", "", "", "", "", "", "")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(0, json.get("successCount").asInt());
        assertTrue(json.get("failureCount").asInt() > 0);
        assertNotNull(json.get("errors"));
    }

    @Test
    void upload_with_duplicate_url_in_db_returns_error() throws Exception {
        createProductAndGetId("already-exists-url");

        byte[] excelBytes = buildValidExcel(
                row("Duplicate URL Product", "", "already-exists-url", "Electronics", "", "",
                        "Desc", "50000", "", "COLOR", "#FF0000", "3", "100", "ACTIVE", "IN_STOCK")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(0, json.get("successCount").asInt());
        assertEquals(1, json.get("failureCount").asInt());
        assertTrue(json.get("errors").get(0).get("message").asText().contains("already exists"));
    }

    @Test
    void upload_with_nonexistent_category_returns_error() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Bad Category", "", "bad-cat-url", "FakeCategory", "", "",
                        "Desc", "99000", "", "COLOR", "#FF0000", "5", "100", "ACTIVE", "IN_STOCK")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(0, json.get("successCount").asInt());
        assertEquals(1, json.get("failureCount").asInt());
        assertTrue(json.get("errors").get(0).get("message").asText().contains("Category not found"));
    }

    @Test
    void upload_requires_admin() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Test", "", "test-url", "Electronics", "", "", "D", "100", "", "COLOR", "#FF0000", "10", "100",
                        "ACTIVE", "IN_STOCK")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_with_specification_columns_saves_specs() throws Exception {
        byte[] excelBytes = buildExcelWithSpecs(
                row("Spec Product", "", "spec-prod-url", "Electronics", "", "",
                        "Desc", "75000", "", "COLOR", "#FF0000", "7", "150", "ACTIVE", "IN_STOCK",
                        "Red", "XL")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        var row = jdbcTemplate.queryForMap(
                "SELECT * FROM product WHERE url = ?", "spec-prod-url");
        assertEquals("Spec Product", row.get("name"));
        assertEquals(7, row.get("inventory_count"));
        assertEquals(150, row.get("weight_gram"));

        String specJson = row.get("specification").toString();
        assertNotNull(specJson);
        assertTrue(specJson.contains("COLOR"));
        assertTrue(specJson.contains("Red"));
        assertTrue(specJson.contains("SIZE"));
        assertTrue(specJson.contains("XL"));

        var priceRow = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", row.get("id"));
        assertEquals(0, new java.math.BigDecimal("750000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertEquals("#FF0000", priceRow.get("variant_value"));
    }

    @Test
    void upload_with_brand_and_subcategory_saves_their_ids() throws Exception {
        Long subCatId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Mobile Accessories') RETURNING id", Long.class);

        byte[] excelBytes = buildValidExcel(
                row("Branded Product", "", "branded-url", "Electronics", "Mobile Accessories",
                        "TestBrand", "SD", "50000", "45000", "COLOR", "#FF0000", "10", "120",
                        "ACTIVE", "IN_STOCK")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        var row = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "branded-url");
        assertEquals("Branded Product", row.get("name"));
        assertEquals(categoryId, row.get("category_id"));
        assertEquals(subCatId, row.get("sub_category_id"));
        assertEquals(brandId, row.get("brand_id"));

        var priceRow = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", row.get("id"));
        assertEquals(0, new java.math.BigDecimal("500000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertEquals(0, new java.math.BigDecimal("450000")
                .compareTo((java.math.BigDecimal) priceRow.get("discount_price")));
    }

    @Test
    void upload_with_custom_variant_value_succeeds() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Custom Variant", "", "custom-variant-url", "Electronics", "", "",
                        "Desc", "99000", "", "COLOR", "#FF5733", "5", "100", "ACTIVE", "IN_STOCK")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(1, json.get("successCount").asInt());
        assertEquals(0, json.get("failureCount").asInt());
    }

    @Test
    void upload_with_variant_type_but_no_variant_value_returns_error() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("No Variant Value", "", "no-var-value-url", "Electronics", "", "",
                        "Desc", "99000", "", "COLOR", "", "5", "100", "ACTIVE", "IN_STOCK")
        );

        MvcResult result = mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = json(result);
        assertEquals(0, json.get("successCount").asInt());
        assertEquals(1, json.get("failureCount").asInt());
        assertTrue(json.get("errors").get(0).get("message").asText()
                .contains("Variant Value is required"));
    }

    @Test
    void upload_lowercase_variant_value_is_normalized_to_uppercase() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Lower Variant", "", "lower-variant-url", "Electronics", "", "",
                        "Desc", "99000", "", "COLOR", "#ff0000", "5", "100", "ACTIVE", "IN_STOCK")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        var row = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "lower-variant-url");
        var priceRow = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", row.get("id"));
        assertEquals("#FF0000", priceRow.get("variant_value"));
    }

    @Test
    void upload_empty_file_returns_error() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        writeHeaderRow(sheet);

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                out.toByteArray()))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_with_matching_code_updates_existing_product_in_place() throws Exception {
        // Create a product first (blank Code → create).
        byte[] createBytes = buildValidExcel(
                row("Original", "", "orig-url", "Electronics", "", "", "Old desc", "100000", "",
                        "COLOR", "#FF0000", "10", "100", "ACTIVE", "IN_STOCK"));
        upload(createBytes).andExpect(status().isOk());

        var created = jdbcTemplate.queryForMap("SELECT id, code FROM product WHERE url = ?", "orig-url");
        String code = (String) created.get("code");
        Object id = created.get("id");

        // Re-upload with that Code, changing name, price, inventory, variant, status and the URL itself.
        byte[] updateBytes = buildExcelWithCode(
                row("Renamed", "", "renamed-url", "Electronics", "", "", "New desc", "250000", "200000",
                        "SIZE", "L", "3", "500", "INACTIVE", "OUT_OF_STOCK", code));
        MvcResult result = upload(updateBytes).andExpect(status().isOk()).andReturn();

        JsonNode json = json(result);
        assertEquals(1, json.get("successCount").asInt(), json.toString());
        assertEquals(0, json.get("createdCount").asInt());
        assertEquals(1, json.get("updatedCount").asInt());

        // Still exactly one product for this code, same id, fields overwritten.
        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE code = ?", Integer.class, code));
        var updated = jdbcTemplate.queryForMap("SELECT * FROM product WHERE code = ?", code);
        assertEquals(id, updated.get("id"));
        assertEquals("Renamed", updated.get("name"));
        assertEquals("renamed-url", updated.get("url"));
        assertEquals("INACTIVE", updated.get("status"));
        assertEquals("OUT_OF_STOCK", updated.get("inventory_status"));
        assertEquals(3, updated.get("inventory_count"));
        assertEquals("SIZE", updated.get("variant_type"));

        // The old URL is now free.
        assertEquals(0, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE url = ?", Integer.class, "orig-url"));

        // Single price row, replaced with the new values.
        assertEquals(1, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_price WHERE product_id = ?", Integer.class, id));
        var priceRow = jdbcTemplate.queryForMap("SELECT * FROM product_price WHERE product_id = ?", id);
        assertEquals(0, new java.math.BigDecimal("2500000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertEquals(0, new java.math.BigDecimal("2000000")
                .compareTo((java.math.BigDecimal) priceRow.get("discount_price")));
        assertEquals("L", priceRow.get("variant_value"));
    }

    @Test
    void upload_with_unknown_code_returns_error() throws Exception {
        byte[] bytes = buildExcelWithCode(
                row("Ghost", "", "ghost-url", "Electronics", "", "", "d", "10000", "",
                        "COLOR", "#FF0000", "1", "10", "ACTIVE", "IN_STOCK", "999-999999"));
        MvcResult result = upload(bytes).andExpect(status().isOk()).andReturn();

        JsonNode json = json(result);
        assertEquals(0, json.get("successCount").asInt());
        assertEquals(1, json.get("failureCount").asInt());
        assertTrue(json.get("errors").get(0).get("message").asText().contains("Product code not found"));
        assertEquals(0, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE url = ?", Integer.class, "ghost-url"));
    }

    @Test
    void upload_mixed_create_and_update_in_one_file() throws Exception {
        byte[] seed = buildValidExcel(
                row("Seed", "", "seed-url", "Electronics", "", "", "d", "100000", "",
                        "COLOR", "#FF0000", "5", "50", "ACTIVE", "IN_STOCK"));
        upload(seed).andExpect(status().isOk());
        String code = jdbcTemplate.queryForObject(
                "SELECT code FROM product WHERE url = ?", String.class, "seed-url");

        byte[] mixed = buildExcelWithCode(
                row("Seed Updated", "", "seed-url", "Electronics", "", "", "d", "120000", "",
                        "COLOR", "#FF0000", "6", "50", "ACTIVE", "IN_STOCK", code),
                row("Brand New", "", "brand-new-url", "Electronics", "", "", "d", "90000", "",
                        "COLOR", "#00FF00", "2", "50", "ACTIVE", "IN_STOCK", "")); // blank Code → create
        MvcResult result = upload(mixed).andExpect(status().isOk()).andReturn();

        JsonNode json = json(result);
        assertEquals(2, json.get("successCount").asInt(), json.toString());
        assertEquals(1, json.get("createdCount").asInt());
        assertEquals(1, json.get("updatedCount").asInt());

        assertEquals(2, (int) jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE url IN ('seed-url','brand-new-url')", Integer.class));
        assertEquals("Seed Updated", jdbcTemplate.queryForObject(
                "SELECT name FROM product WHERE url = 'seed-url'", String.class));
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private ResultActions upload(byte[] excelBytes) throws Exception {
        return mockMvc.perform(multipart("/api/products/upload")
                .file(new MockMultipartFile("file", "products.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.MULTIPART_FORM_DATA));
    }

    private String[] row(String... values) {
        return values;
    }

    private byte[] buildValidExcel(String[]... rows) throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        writeHeaderRow(sheet);

        for (int i = 0; i < rows.length; i++) {
            var dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < rows[i].length; j++) {
                dataRow.createCell(j).setCellValue(rows[i][j]);
            }
        }

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private byte[] buildExcelWithCode(String[]... rows) throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var header = sheet.createRow(0);
        String[] headers = {
                "Name", "Local Name", "URL", "Category", "Sub Category", "Brand",
                "Short Description", "Price", "Discount Price",
                "Variant Type", "Variant Value", "Inventory Count", "Weight (grams)", "Status",
                "Inventory Status", "Code"
        };
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
        for (int i = 0; i < rows.length; i++) {
            var dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < rows[i].length; j++) {
                dataRow.createCell(j).setCellValue(rows[i][j]);
            }
        }

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private byte[] buildExcelWithSpecs(String[]... rows) throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        writeHeaderRow(sheet);

        var headerRow = sheet.getRow(0);
        headerRow.createCell(15).setCellValue("Spec:COLOR");
        headerRow.createCell(16).setCellValue("Spec:SIZE");

        for (int i = 0; i < rows.length; i++) {
            var dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < rows[i].length; j++) {
                dataRow.createCell(j).setCellValue(rows[i][j]);
            }
        }

        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    private void writeHeaderRow(Sheet sheet) {
        var row = sheet.createRow(0);
        String[] headers = {
                "Name", "Local Name", "URL", "Category", "Sub Category", "Brand",
                "Short Description", "Price", "Discount Price",
                "Variant Type", "Variant Value", "Inventory Count", "Weight (grams)", "Status", "Inventory Status"
        };
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }
}
