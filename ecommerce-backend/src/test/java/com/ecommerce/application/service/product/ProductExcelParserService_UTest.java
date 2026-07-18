package com.ecommerce.application.service.product;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductExcelParserService_UTest {

    private ProductExcelParserService service;
    private Set<String> requiredHeaders;

    @BeforeEach
    void setUp() {
        service = new ProductExcelParserService();
        requiredHeaders = new HashSet<>(ProductExcelTemplateService.requiredHeaders());
    }

    @Test
    void parses_valid_file_with_multiple_rows() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");

        var dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Product A");
        dataRow1.createCell(1).setCellValue("product-a");
        dataRow1.createCell(2).setCellValue("Electronics");
        dataRow1.createCell(3).setCellValue("100");
        dataRow1.createCell(4).setCellValue("10");

        var dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("Product B");
        dataRow2.createCell(1).setCellValue("product-b");
        dataRow2.createCell(2).setCellValue("Clothing");
        dataRow2.createCell(3).setCellValue("200");
        dataRow2.createCell(4).setCellValue("5");

        var file = toMultipartFile(workbook);

        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(2, rows.size());
        assertEquals("Product A", rows.get(0).get("Name"));
        assertEquals("product-a", rows.get(0).get("URL"));
        assertEquals("Electronics", rows.get(0).get("Category"));
        assertEquals("Product B", rows.get(1).get("Name"));
        assertEquals("5", rows.get(1).get("Inventory Count"));
    }

    @Test
    void parses_file_with_specification_columns() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");
        headerRow.createCell(5).setCellValue("Spec:COLOR");
        headerRow.createCell(6).setCellValue("Spec:SIZE");

        var dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Product C");
        dataRow.createCell(1).setCellValue("product-c");
        dataRow.createCell(2).setCellValue("Accessories");
        dataRow.createCell(3).setCellValue("150");
        dataRow.createCell(4).setCellValue("3");
        dataRow.createCell(5).setCellValue("Red");
        dataRow.createCell(6).setCellValue("XL");

        var file = toMultipartFile(workbook);
        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(1, rows.size());
        assertEquals("Red", rows.get(0).get("Spec:COLOR"));
        assertEquals("XL", rows.get(0).get("Spec:SIZE"));
    }

    @Test
    void empty_file_throws_empty_file_exception() throws Exception {
        var workbook = new XSSFWorkbook();
        workbook.createSheet(); // empty sheet with no rows
        var file = toMultipartFile(workbook);

        var ex = assertThrows(EcommerceException.class,
                () -> service.parse(file, requiredHeaders));
        assertEquals(ECOMErrorType.EXCEL_EMPTY_FILE, ex.getEcomErrorType());
    }

    @Test
    void header_only_file_throws_empty_file_exception() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");

        var file = toMultipartFile(workbook);
        var ex = assertThrows(EcommerceException.class,
                () -> service.parse(file, requiredHeaders));
        assertEquals(ECOMErrorType.EXCEL_EMPTY_FILE, ex.getEcomErrorType());
    }

    @Test
    void missing_required_column_throws_invalid_header_exception() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Category");
        // Missing: URL, Price, Inventory Count
        headerRow.createCell(2).setCellValue("Some Other");

        var dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Test");

        var file = toMultipartFile(workbook);
        var ex = assertThrows(EcommerceException.class,
                () -> service.parse(file, requiredHeaders));
        assertEquals(ECOMErrorType.EXCEL_INVALID_HEADER, ex.getEcomErrorType());
    }

    @Test
    void handles_empty_cells() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");
        headerRow.createCell(5).setCellValue("Local Name");

        var dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Product D");
        dataRow.createCell(1).setCellValue("product-d");
        dataRow.createCell(2).setCellValue("Books");
        dataRow.createCell(3).setCellValue("99");
        dataRow.createCell(4).setCellValue("20");
        // Local Name is intentionally left empty

        var file = toMultipartFile(workbook);
        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(1, rows.size());
        assertEquals("Product D", rows.get(0).get("Name"));
        // Local Name should be null (empty cell)
        assertTrue(rows.get(0).get("Local Name") == null);
    }

    @Test
    void skips_completely_empty_data_rows() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");

        var dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Valid Product");
        dataRow1.createCell(1).setCellValue("valid-product");
        dataRow1.createCell(2).setCellValue("Cat");
        dataRow1.createCell(3).setCellValue("100");
        dataRow1.createCell(4).setCellValue("10");

        // Row 2 is completely empty
        sheet.createRow(2);

        var dataRow3 = sheet.createRow(3);
        dataRow3.createCell(0).setCellValue("Another Product");
        dataRow3.createCell(1).setCellValue("another-product");
        dataRow3.createCell(2).setCellValue("Cat");
        dataRow3.createCell(3).setCellValue("200");
        dataRow3.createCell(4).setCellValue("20");

        var file = toMultipartFile(workbook);
        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(2, rows.size());
        assertEquals("Valid Product", rows.get(0).get("Name"));
        assertEquals("Another Product", rows.get(1).get("Name"));
    }

    @Test
    void row_numbers_are_one_based() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");

        var dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("Row Test");
        dataRow.createCell(1).setCellValue("row-test");
        dataRow.createCell(2).setCellValue("Cat");
        dataRow.createCell(3).setCellValue("50");
        dataRow.createCell(4).setCellValue("5");

        var file = toMultipartFile(workbook);
        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(1, rows.size());
        assertEquals(2, rows.get(0).rowNumber); // Row 1 in Excel (0-indexed) = row 2 for user
    }

    @Test
    void handles_boundary_string_values() throws Exception {
        var workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet();
        var headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("URL");
        headerRow.createCell(2).setCellValue("Category");
        headerRow.createCell(3).setCellValue("Price");
        headerRow.createCell(4).setCellValue("Inventory Count");
        headerRow.createCell(5).setCellValue("Short Description");

        var dataRow = sheet.createRow(1);
        // Extremely long name
        String longName = "A".repeat(500);
        dataRow.createCell(0).setCellValue(longName);
        dataRow.createCell(1).setCellValue("boundary-test");
        dataRow.createCell(2).setCellValue("Cat");
        dataRow.createCell(3).setCellValue("0.01");
        dataRow.createCell(4).setCellValue("0");
        dataRow.createCell(5).setCellValue("A".repeat(2000));

        var file = toMultipartFile(workbook);
        List<ExcelProductRow> rows = service.parse(file, requiredHeaders);

        assertEquals(1, rows.size());
        assertEquals(longName, rows.get(0).get("Name"));
        assertEquals("0.01", rows.get(0).get("Price"));
        assertEquals("0", rows.get(0).get("Inventory Count"));
    }

    private MockMultipartFile toMultipartFile(XSSFWorkbook workbook) throws Exception {
        var out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new MockMultipartFile("file", "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());
    }
}
