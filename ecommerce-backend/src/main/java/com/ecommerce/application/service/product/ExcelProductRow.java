package com.ecommerce.application.service.product;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Raw cell values for a single row, keyed by column header name.
 * Package-private — only the Excel parser and batch service need it.
 */
class ExcelProductRow {

    final int rowNumber;
    final Map<String, String> cells;

    ExcelProductRow(int rowNumber) {
        this.rowNumber = rowNumber;
        this.cells = new LinkedHashMap<>();
    }

    void put(String header, String value) {
        cells.put(header, value != null ? value.strip() : null);
    }

    String get(String header) {
        String v = cells.get(header);
        return v == null || v.isEmpty() ? null : v;
    }
}
