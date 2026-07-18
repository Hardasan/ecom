package com.ecommerce.application.service.product;

import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductExcelParserService {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    public List<ExcelProductRow> parse(MultipartFile file, Set<String> requiredHeaders) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                throw new EcommerceException(ECOMErrorType.EXCEL_EMPTY_FILE);
            }

            List<String> headers = readHeaders(sheet.getRow(0));
            if (headers.isEmpty()) {
                throw new EcommerceException(ECOMErrorType.EXCEL_EMPTY_FILE);
            }

            List<String> missing = requiredHeaders.stream()
                    .filter(h -> !headers.contains(h))
                    .collect(Collectors.toList());
            if (!missing.isEmpty()) {
                throw new EcommerceException(ECOMErrorType.EXCEL_INVALID_HEADER,
                        new Object[]{String.join(", ", missing)});
            }

            List<ExcelProductRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ExcelProductRow excelRow = new ExcelProductRow(i + 1); // 1-based for user display
                boolean hasValue = false;
                for (int col = 0; col < headers.size(); col++) {
                    Cell cell = row.getCell(col);
                    String value = cell == null ? null : DATA_FORMATTER.formatCellValue(cell).strip();
                    if (value != null && !value.isEmpty()) {
                        hasValue = true;
                    }
                    excelRow.put(headers.get(col), value);
                }
                if (hasValue) {
                    rows.add(excelRow);
                }
            }

            if (rows.isEmpty()) {
                throw new EcommerceException(ECOMErrorType.EXCEL_EMPTY_FILE);
            }

            return rows;
        } catch (EcommerceException e) {
            throw e;
        } catch (Exception e) {
            throw new EcommerceException(ECOMErrorType.EXCEL_PARSING_ERROR);
        }
    }

    private List<String> readHeaders(Row headerRow) {
        if (headerRow == null) return List.of();
        List<String> headers = new ArrayList<>();
        for (int col = 0; col < headerRow.getLastCellNum(); col++) {
            Cell cell = headerRow.getCell(col);
            String value = cell == null ? "" : DATA_FORMATTER.formatCellValue(cell).strip();
            headers.add(value);
        }
        return headers;
    }
}
