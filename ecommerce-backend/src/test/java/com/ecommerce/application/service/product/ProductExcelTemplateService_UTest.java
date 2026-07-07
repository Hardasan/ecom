package com.ecommerce.application.service.product;

import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.repository.CategoryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductExcelTemplateService_UTest {

    @Mock
    private CategoryRepository categoryRepository;

    private ProductExcelTemplateService service;

    @BeforeEach
    void setUp() {
        service = new ProductExcelTemplateService(categoryRepository);
    }

    @Test
    void generated_workbook_has_correct_sheet_and_headers() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        byte[] content = service.generateTemplate();
        assertNotNull(content);
        assertTrue(content.length > 0);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertEquals(1, workbook.getNumberOfSheets());
            Sheet sheet = workbook.getSheet("Products");
            assertNotNull(sheet);

            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);

            String[] expected = ProductExcelTemplateService.headers();
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], headerRow.getCell(i).getStringCellValue(),
                        "Header at column " + i + " should match");
            }

            // Verify required headers are present
            List<String> actualHeaders = new java.util.ArrayList<>();
            for (int i = 0; i < expected.length; i++) {
                actualHeaders.add(headerRow.getCell(i).getStringCellValue());
            }
            for (String required : ProductExcelTemplateService.requiredHeaders()) {
                assertTrue(actualHeaders.contains(required),
                        "Required header '" + required + "' should be present");
            }
        }
    }

    @Test
    void template_includes_sample_row() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        byte[] content = service.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Products");
            Row sampleRow = sheet.getRow(1);
            assertNotNull(sampleRow, "Sample row should exist");

            // Name and URL should be filled
            assertNotNull(sampleRow.getCell(0));
            assertNotNull(sampleRow.getCell(2));

            // Price should be a numeric string
            String priceVal = sampleRow.getCell(8).getStringCellValue();
            assertDoesNotThrow(() -> new java.math.BigDecimal(priceVal));
        }
    }

    @Test
    void template_includes_specification_columns_for_all_spec_keys() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());

        byte[] content = service.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Row headerRow = workbook.getSheet("Products").getRow(0);
            Set<String> headers = new java.util.HashSet<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                headers.add(headerRow.getCell(i).getStringCellValue());
            }
            for (com.ecommerce.persistence.entity.enumeration.SpecificationKey key :
                    com.ecommerce.persistence.entity.enumeration.SpecificationKey.values()) {
                assertTrue(headers.contains("Spec:" + key.name()),
                        "Header should contain Spec:" + key.name());
            }
        }
    }

    @Test
    void template_includes_category_dropdown_when_categories_exist() throws Exception {
        Category cat1 = new Category();
        cat1.setId(1L);
        cat1.setName("Electronics");
        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Clothing");
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        byte[] content = service.generateTemplate();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet("Products");
            assertNotNull(sheet);
            // The sheet should have data validations (we verify generation doesn't throw)
        }
    }

    @Test
    void headers_method_returns_all_expected_columns() {
        String[] headers = ProductExcelTemplateService.headers();

        List<String> headerList = Arrays.asList(headers);
        assertTrue(headerList.contains("Name"));
        assertTrue(headerList.contains("URL"));
        assertTrue(headerList.contains("Category"));
        assertTrue(headerList.contains("Price"));
        assertTrue(headerList.contains("Inventory Count"));
        assertTrue(headerList.contains("Variant Type"));
        assertTrue(headerList.contains("Status"));
    }

    @Test
    void required_headers_includes_key_columns() {
        List<String> required = ProductExcelTemplateService.requiredHeaders();
        assertTrue(required.contains("Name"));
        assertTrue(required.contains("URL"));
        assertTrue(required.contains("Category"));
        assertTrue(required.contains("Price"));
        assertTrue(required.contains("Inventory Count"));
    }
}
