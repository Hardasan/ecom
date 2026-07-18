package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.BatchProductRowError;
import com.ecommerce.application.api.dto.product.BatchProductUploadResult;
import com.ecommerce.persistence.entity.Brand;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.entity.Price;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.enumeration.InventoryStatus;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.entity.enumeration.SpecificationKey;
import com.ecommerce.persistence.entity.enumeration.VariantType;
import com.ecommerce.persistence.repository.BrandRepository;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private static final Logger log = LoggerFactory.getLogger(ProductBatchService.class);

    private final ProductExcelParserService parserService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public BatchProductUploadResult processUpload(MultipartFile file) {
        long start = System.currentTimeMillis();

        Set<String> requiredHeaders = new HashSet<>(ProductExcelTemplateService.requiredHeaders());
        List<ExcelProductRow> rows = parserService.parse(file, requiredHeaders);

        // Category/brand names are not unique (no DB constraint), so two rows can collapse to the
        // same lower-cased key. Keep the first-seen id instead of letting toMap throw on collision.
        Map<String, Long> categoryNameToId = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), Category::getId, (first, dup) -> first));
        Map<String, Long> brandNameToId = brandRepository.findAll().stream()
                .collect(Collectors.toMap(b -> b.getName().toLowerCase(), Brand::getId, (first, dup) -> first));
        Set<String> existingUrls = new HashSet<>(productRepository.findAllUrls());

        // A row with a non-blank Code updates the matching product; a blank Code creates a new one.
        Set<String> codesInFile = rows.stream()
                .map(r -> r.get("Code"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, Product> codeToProduct = codesInFile.isEmpty()
                ? Map.of()
                : productRepository.findByCodeIn(codesInFile).stream()
                        .collect(Collectors.toMap(Product::getCode, p -> p));

        Set<String> inFileUrls = new HashSet<>();
        Set<String> inFileCodes = new HashSet<>();

        List<BatchProductRowError> errors = new ArrayList<>();
        List<Product> toSave = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        for (ExcelProductRow row : rows) {
            String code = row.get("Code");
            Product existing = code == null ? null : codeToProduct.get(code);

            List<BatchProductRowError> rowErrors = validateAndCollect(row, categoryNameToId, brandNameToId,
                    existingUrls, inFileUrls, inFileCodes, existing);
            if (rowErrors.isEmpty()) {
                Product product = existing != null ? existing : new Product();
                applyRow(product, row, categoryNameToId, brandNameToId);
                if (existing == null) {
                    product.setCode(generateCode(product.getCategoryId()));
                    createdCount++;
                } else {
                    updatedCount++;
                }
                toSave.add(product);
                inFileUrls.add(product.getUrl());
                if (code != null) {
                    inFileCodes.add(code);
                }
            } else {
                errors.addAll(rowErrors);
            }
        }

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Batch product upload complete: totalRows={}, created={}, updated={}, failureCount={}, elapsedMs={}",
                rows.size(), createdCount, updatedCount, errors.size(), elapsed);

        return new BatchProductUploadResult(
                rows.size(),
                createdCount + updatedCount,
                createdCount,
                updatedCount,
                errors.size(),
                errors.isEmpty() ? null : errors,
                elapsed);
    }

    private List<BatchProductRowError> validateAndCollect(ExcelProductRow row,
                                                          Map<String, Long> categoryNameToId,
                                                          Map<String, Long> brandNameToId,
                                                          Set<String> existingUrls,
                                                          Set<String> inFileUrls,
                                                          Set<String> inFileCodes,
                                                          Product existing) {
        List<BatchProductRowError> errs = new ArrayList<>();
        int rn = row.rowNumber;

        String code = row.get("Code");
        if (code != null) {
            if (inFileCodes.contains(code)) {
                errs.add(new BatchProductRowError(rn, "Code", "Duplicate Code within file: " + code));
            } else if (existing == null) {
                errs.add(new BatchProductRowError(rn, "Code", "Product code not found: " + code));
            }
        }

        String name = row.get("Name");
        if (name == null || name.isBlank()) {
            errs.add(new BatchProductRowError(rn, "Name", "Name is required"));
        } else if (name.length() > 255) {
            errs.add(new BatchProductRowError(rn, "Name", "Name exceeds 255 characters"));
        }

        String url = row.get("URL");
        if (url == null || url.isBlank()) {
            errs.add(new BatchProductRowError(rn, "URL", "URL is required"));
        } else if (url.length() > 255) {
            errs.add(new BatchProductRowError(rn, "URL", "URL exceeds 255 characters"));
        } else if (inFileUrls.contains(url)) {
            errs.add(new BatchProductRowError(rn, "URL", "Duplicate URL within file: " + url));
        } else if (existingUrls.contains(url) && (existing == null || !url.equals(existing.getUrl()))) {
            // Conflict only when the URL belongs to a *different* product; keeping/renaming your own is fine.
            errs.add(new BatchProductRowError(rn, "URL", "A product with this URL already exists: " + url));
        }

        String categoryName = row.get("Category");
        if (categoryName == null || categoryName.isBlank()) {
            errs.add(new BatchProductRowError(rn, "Category", "Category is required"));
        } else if (!categoryNameToId.containsKey(categoryName.toLowerCase())) {
            errs.add(new BatchProductRowError(rn, "Category", "Category not found: " + categoryName));
        }

        String subCategoryName = row.get("Sub Category");
        if (subCategoryName != null && !subCategoryName.isBlank()
                && !categoryNameToId.containsKey(subCategoryName.toLowerCase())) {
            errs.add(new BatchProductRowError(rn, "Sub Category", "Sub category not found: " + subCategoryName));
        }

        String brandName = row.get("Brand");
        if (brandName != null && !brandName.isBlank()
                && !brandNameToId.containsKey(brandName.toLowerCase())) {
            errs.add(new BatchProductRowError(rn, "Brand", "Brand not found: " + brandName));
        }

        String priceStr = row.get("Price");
        if (priceStr == null || priceStr.isBlank()) {
            errs.add(new BatchProductRowError(rn, "Price", "Price is required"));
        } else {
            try {
                BigDecimal price = new BigDecimal(priceStr);
                if (price.compareTo(BigDecimal.ZERO) <= 0) {
                    errs.add(new BatchProductRowError(rn, "Price", "Price must be greater than zero"));
                }
            } catch (NumberFormatException e) {
                errs.add(new BatchProductRowError(rn, "Price", "Invalid price format: " + priceStr));
            }
        }

        String discountStr = row.get("Discount Price");
        if (discountStr != null && !discountStr.isBlank()) {
            try {
                BigDecimal discount = new BigDecimal(discountStr);
                if (discount.compareTo(BigDecimal.ZERO) < 0) {
                    errs.add(new BatchProductRowError(rn, "Discount Price", "Discount price must be >= 0"));
                }
            } catch (NumberFormatException e) {
                errs.add(new BatchProductRowError(rn, "Discount Price", "Invalid discount price format: " + discountStr));
            }
        }

        String variantStr = row.get("Variant Type");
        VariantType variantType = null;
        boolean variantTypeValid = false;
        if (variantStr != null && !variantStr.isBlank()) {
            try {
                variantType = VariantType.valueOf(variantStr.toUpperCase());
                variantTypeValid = true;
            } catch (IllegalArgumentException e) {
                errs.add(new BatchProductRowError(rn, "Variant Type",
                        "Invalid variant type: " + variantStr + ". Valid values: " +
                                java.util.Arrays.toString(VariantType.values())));
            }
        }

        if (variantTypeValid) {
            String variantValueStr = row.get("Variant Value");
            if (variantValueStr == null || variantValueStr.isBlank()) {
                errs.add(new BatchProductRowError(rn, "Variant Value",
                        "Variant Value is required when Variant Type is provided"));
            }
        } else {
            String variantValueStr = row.get("Variant Value");
            if (variantValueStr != null && !variantValueStr.isBlank()) {
                errs.add(new BatchProductRowError(rn, "Variant Value",
                        "Variant Value requires Variant Type to be set"));
            }
        }

        String inventoryStr = row.get("Inventory Count");
        if (inventoryStr == null || inventoryStr.isBlank()) {
            errs.add(new BatchProductRowError(rn, "Inventory Count", "Inventory count is required"));
        } else {
            try {
                int inv = Integer.parseInt(inventoryStr);
                if (inv < 0) {
                    errs.add(new BatchProductRowError(rn, "Inventory Count", "Inventory count must be >= 0"));
                }
            } catch (NumberFormatException e) {
                errs.add(new BatchProductRowError(rn, "Inventory Count", "Invalid inventory count: " + inventoryStr));
            }
        }

        String weightStr = row.get("Weight (grams)");
        if (weightStr != null && !weightStr.isBlank()) {
            try {
                int w = Integer.parseInt(weightStr);
                if (w < 0) {
                    errs.add(new BatchProductRowError(rn, "Weight (grams)", "Weight must be >= 0"));
                }
            } catch (NumberFormatException e) {
                errs.add(new BatchProductRowError(rn, "Weight (grams)", "Invalid weight: " + weightStr));
            }
        }

        String statusStr = row.get("Status");
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                ProductStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errs.add(new BatchProductRowError(rn, "Status",
                        "Invalid status: " + statusStr + ". Valid: ACTIVE, INACTIVE"));
            }
        }

        String invStatusStr = row.get("Inventory Status");
        if (invStatusStr != null && !invStatusStr.isBlank()) {
            try {
                InventoryStatus.valueOf(invStatusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errs.add(new BatchProductRowError(rn, "Inventory Status",
                        "Invalid inventory status: " + invStatusStr + ". Valid: IN_STOCK, LOW_STOCK, OUT_OF_STOCK"));
            }
        }

        return errs;
    }

    /**
     * Copies the row onto {@code product}, which is either a fresh entity (create) or an existing
     * managed one (update). The row is the source of truth: blank optional cells clear the field.
     * ({@code ExcelProductRow.get} already returns null for blank cells, so a null check is enough.)
     */
    private void applyRow(Product product, ExcelProductRow row,
                          Map<String, Long> categoryNameToId,
                          Map<String, Long> brandNameToId) {
        product.setName(row.get("Name"));
        product.setLocalName(row.get("Local Name"));
        product.setUrl(row.get("URL"));

        product.setCategoryId(categoryNameToId.get(row.get("Category").toLowerCase()));

        String subCat = row.get("Sub Category");
        product.setSubCategoryId(subCat != null ? categoryNameToId.get(subCat.toLowerCase()) : null);

        String brand = row.get("Brand");
        product.setBrandId(brand != null ? brandNameToId.get(brand.toLowerCase()) : null);

        product.setShortDescription(row.get("Short Description"));
        product.setFullDescription(row.get("Full Description"));

        Price price = new Price();
        price.setPrice(new BigDecimal(row.get("Price")));

        String discountStr = row.get("Discount Price");
        if (discountStr != null) {
            price.setDiscountPrice(new BigDecimal(discountStr));
        }

        String variantTypeStr = row.get("Variant Type");
        product.setVariantType(variantTypeStr != null
                ? VariantType.valueOf(variantTypeStr.trim().toUpperCase()) : null);

        String variantValueStr = row.get("Variant Value");
        price.setVariantValue(variantValueStr != null ? variantValueStr.trim().toUpperCase() : null);

        // Mutate the existing collection (clear + add) so Hibernate's element-collection bag is reused
        // rather than replaced — matters when updating an already-managed product.
        product.getPrices().clear();
        product.getPrices().add(price);

        product.setInventoryCount(Integer.parseInt(row.get("Inventory Count")));

        String weight = row.get("Weight (grams)");
        product.setWeightGram(weight != null ? Integer.parseInt(weight) : 0);

        String statusStr = row.get("Status");
        product.setStatus(statusStr != null
                ? ProductStatus.valueOf(statusStr.toUpperCase()) : ProductStatus.ACTIVE);

        String invStatusStr = row.get("Inventory Status");
        product.setInventoryStatus(invStatusStr != null
                ? InventoryStatus.valueOf(invStatusStr.toUpperCase()) : InventoryStatus.IN_STOCK);

        Map<SpecificationKey, String> specs = new HashMap<>();
        for (SpecificationKey key : SpecificationKey.values()) {
            String val = row.get("Spec:" + key.name());
            if (val != null) {
                specs.put(key, val);
            }
        }
        product.setSpecification(specs);
    }

    private String generateCode(Long categoryId) {
        Long seq = jdbcTemplate.queryForObject("SELECT NEXTVAL('product_code_seq')", Long.class);
        return categoryId + "-" + seq;
    }
}
