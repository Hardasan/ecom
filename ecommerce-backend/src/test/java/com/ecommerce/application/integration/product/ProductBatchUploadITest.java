package com.ecommerce.application.integration.product;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                        "Desc", "Full desc", "199000", "", "COLOR", "15", "200", "ACTIVE", "IN_STOCK")
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
        assertEquals("Full desc", row.get("full_description"));
        assertEquals(15, row.get("inventory_count"));
        assertEquals(200, row.get("weight_gram"));
        assertEquals("ACTIVE", row.get("status"));
        assertEquals("IN_STOCK", row.get("inventory_status"));
        assertNotNull(row.get("created_at"));
        assertNotNull(row.get("updated_at"));
        // code format: {categoryId}-{seq}
        String code = (String) row.get("code");
        assertTrue(code.matches(categoryId + "-\\d+"), "Expected code like " + categoryId + "-N, got: " + code);

        // Verify price in product_price table
        var priceRow = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", row.get("id"));
        assertEquals(0, new java.math.BigDecimal("199000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertNull(priceRow.get("discount_price"));
        assertEquals("COLOR", priceRow.get("variant_type"));
    }

    @Test
    void upload_multiple_products_all_saved() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Multi 1", "", "multi-1", "Electronics", "", "", "D1", "F1", "100000", "", "COLOR", "5", "100", "ACTIVE", "IN_STOCK"),
                row("Multi 2", "", "multi-2", "Electronics", "", "", "D2", "F2", "200000", "180000", "SIZE", "8", "150", "ACTIVE", "IN_STOCK"),
                row("Multi 3", "", "multi-3", "Electronics", "", "", "D3", "F3", "300000", "", "STYLE", "12", "200", "ACTIVE", "IN_STOCK")
        );

        mockMvc.perform(multipart("/api/products/upload")
                        .file(new MockMultipartFile("file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                excelBytes))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

        int count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product WHERE url IN ('multi-1','multi-2','multi-3')", Integer.class);
        assertEquals(3, count);

        // Verify Multi 2 has discount price and SIZE variant
        var multi2 = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "multi-2");
        assertEquals("Multi 2", multi2.get("name"));
        assertEquals(8, multi2.get("inventory_count"));
        assertEquals(150, multi2.get("weight_gram"));

        var price2 = jdbcTemplate.queryForMap(
                "SELECT * FROM product_price WHERE product_id = ?", multi2.get("id"));
        assertEquals(0, new java.math.BigDecimal("200000")
                .compareTo((java.math.BigDecimal) price2.get("price")));
        assertEquals(0, new java.math.BigDecimal("180000")
                .compareTo((java.math.BigDecimal) price2.get("discount_price")));
        assertEquals("SIZE", price2.get("variant_type"));

        // Verify Multi 1 has null local_name (empty in Excel → null in DB)
        var multi1 = jdbcTemplate.queryForMap("SELECT * FROM product WHERE url = ?", "multi-1");
        assertNull(multi1.get("local_name"));
    }

    @Test
    void upload_with_missing_required_fields_returns_errors() throws Exception {
        byte[] excelBytes = buildValidExcel(
                row("Incomplete", "", "", "", "", "", "", "", "", "", "", "", "", "", "")
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
                        "Desc", "Full", "50000", "", "COLOR", "3", "100", "ACTIVE", "IN_STOCK")
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
                        "Desc", "Full", "99000", "", "COLOR", "5", "100", "ACTIVE", "IN_STOCK")
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
                row("Test", "", "test-url", "Electronics", "", "", "D", "F", "100", "", "COLOR", "10", "100", "ACTIVE", "IN_STOCK")
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
                        "Desc", "Full", "75000", "", "COLOR", "7", "150", "ACTIVE", "IN_STOCK",
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
        assertEquals(0, new java.math.BigDecimal("75000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertEquals("COLOR", priceRow.get("variant_type"));
    }

    @Test
    void upload_with_brand_and_subcategory_saves_their_ids() throws Exception {
        Long subCatId = jdbcTemplate.queryForObject(
                "INSERT INTO category (name) VALUES ('Mobile Accessories') RETURNING id", Long.class);

        byte[] excelBytes = buildValidExcel(
                row("Branded Product", "", "branded-url", "Electronics", "Mobile Accessories",
                        "TestBrand", "SD", "FD", "50000", "45000", "COLOR", "10", "120",
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
        assertEquals(0, new java.math.BigDecimal("50000")
                .compareTo((java.math.BigDecimal) priceRow.get("price")));
        assertEquals(0, new java.math.BigDecimal("45000")
                .compareTo((java.math.BigDecimal) priceRow.get("discount_price")));
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

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

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
                "Short Description", "Full Description", "Price", "Discount Price",
                "Variant Type", "Inventory Count", "Weight (grams)", "Status", "Inventory Status"
        };
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
    }
}
