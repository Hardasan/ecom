package com.ecommerce.application.service.product;

import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.SpecificationKey;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductExcelTemplateService {

    private static final String[] HEADERS = buildHeaders();
    private static final List<String> REQUIRED_HEADERS = List.of("Name", "URL", "Category", "Price", "Inventory Count");

    private final CategoryRepository categoryRepository;

    public byte[] generateTemplate() {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Products");
            sheet.setDefaultColumnWidth(18);

            writeHeaderRow(sheet);
            writeSampleRow(sheet);
            addValidations(sheet);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel template", e);
        }
    }

    public static String[] headers() {
        return HEADERS.clone();
    }

    public static List<String> requiredHeaders() {
        return REQUIRED_HEADERS;
    }

    private void writeHeaderRow(Sheet sheet) {
        var row = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            var cell = row.createCell(i);
            cell.setCellValue(HEADERS[i]);
            var style = sheet.getWorkbook().createCellStyle();
            var font = sheet.getWorkbook().createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
    }

    private void writeSampleRow(Sheet sheet) {
        var row = sheet.createRow(1);
        row.createCell(0).setCellValue("Sample Product");
        row.createCell(1).setCellValue("محصول نمونه");
        row.createCell(2).setCellValue("sample-product");
        row.createCell(3).setCellValue(""); // Category — dropdown
        row.createCell(4).setCellValue(""); // Sub Category
        row.createCell(5).setCellValue(""); // Brand
        row.createCell(6).setCellValue("A short description of the product");
        row.createCell(7).setCellValue("Full detailed description");
        row.createCell(8).setCellValue("1000000");
        row.createCell(9).setCellValue("900000");
        row.createCell(10).setCellValue("COLOR");
        row.createCell(11).setCellValue("50");
        row.createCell(12).setCellValue("200");
        row.createCell(13).setCellValue("ACTIVE");
        row.createCell(14).setCellValue("IN_STOCK");
        int col = 15;
        for (SpecificationKey key : SpecificationKey.values()) {
            row.createCell(col++).setCellValue(key.name() + " value");
        }
    }

    private void addValidations(Sheet sheet) {
        XSSFSheet xssfSheet = ((SXSSFWorkbook) sheet.getWorkbook()).getXSSFWorkbook().getSheet(sheet.getSheetName());
        DataValidationHelper helper = new XSSFDataValidationHelper(xssfSheet);

        List<String> categories = categoryRepository.findAll().stream()
                .map(Category::getName)
                .collect(Collectors.toList());

        if (!categories.isEmpty()) {
            addListValidation(xssfSheet, helper, 3, categories, 500); // Category column
        }

        String[] variantTypes = Arrays.stream(VariantType.values()).map(Enum::name).toArray(String[]::new);
        addListValidation(xssfSheet, helper, 10, Arrays.asList(variantTypes), 500);

        String[] productStatuses = Arrays.stream(ProductStatus.values()).map(Enum::name).toArray(String[]::new);
        addListValidation(xssfSheet, helper, 13, Arrays.asList(productStatuses), 500);

        String[] inventoryStatuses = Arrays.stream(InventoryStatus.values()).map(Enum::name).toArray(String[]::new);
        addListValidation(xssfSheet, helper, 14, Arrays.asList(inventoryStatuses), 500);
    }

    private void addListValidation(XSSFSheet sheet, DataValidationHelper helper, int colIndex,
                                   List<String> values, int maxRows) {
        if (values.isEmpty()) return;
        DataValidationConstraint constraint =
                helper.createExplicitListConstraint(values.toArray(new String[0]));
        CellRangeAddressList addressList = new CellRangeAddressList(1, maxRows, colIndex, colIndex);
        DataValidation validation = helper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        sheet.addValidationData(validation);
    }

    private static String[] buildHeaders() {
        String[] base = {
                "Name", "Local Name", "URL", "Category", "Sub Category", "Brand",
                "Short Description", "Full Description", "Price", "Discount Price",
                "Variant Type", "Inventory Count", "Weight (grams)", "Status", "Inventory Status"
        };
        SpecificationKey[] specs = SpecificationKey.values();
        String[] headers = Arrays.copyOf(base, base.length + specs.length);
        for (int i = 0; i < specs.length; i++) {
            headers[base.length + i] = "Spec:" + specs[i].name();
        }
        return headers;
    }
}
