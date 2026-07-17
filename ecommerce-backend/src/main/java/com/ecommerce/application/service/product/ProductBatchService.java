package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.BatchProductRowError;
import com.ecommerce.application.api.dto.product.BatchProductUploadResult;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

        Map<String, Long> categoryNameToId = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), Category::getId));
        Map<String, Long> brandNameToId = brandRepository.findAll().stream()
                .collect(Collectors.toMap(b -> b.getName().toLowerCase(), b -> b.getId()));
        Set<String> existingUrls = new HashSet<>(productRepository.findAllUrls());
        Set<String> inFileUrls = new HashSet<>();

        List<BatchProductRowError> errors = new ArrayList<>();
        List<Product> validProducts = new ArrayList<>();

        for (ExcelProductRow row : rows) {
            List<BatchProductRowError> rowErrors = validateAndCollect(row, categoryNameToId, brandNameToId,
                    existingUrls, inFileUrls);
            if (rowErrors.isEmpty()) {
                Product product = mapToProduct(row, categoryNameToId, brandNameToId);
                product.setCode(generateCode(product.getCategoryId()));
                validProducts.add(product);
                inFileUrls.add(product.getUrl());
            } else {
                errors.addAll(rowErrors);
            }
        }

        if (!validProducts.isEmpty()) {
            productRepository.saveAll(validProducts);
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Batch product upload complete: totalRows={}, successCount={}, failureCount={}, elapsedMs={}",
                rows.size(), validProducts.size(), errors.size(), elapsed);

        return new BatchProductUploadResult(
                rows.size(),
                validProducts.size(),
                errors.size(),
                errors.isEmpty() ? null : errors,
                elapsed);
    }

    private List<BatchProductRowError> validateAndCollect(ExcelProductRow row,
                                                          Map<String, Long> categoryNameToId,
                                                          Map<String, Long> brandNameToId,
                                                          Set<String> existingUrls,
                                                          Set<String> inFileUrls) {
        List<BatchProductRowError> errs = new ArrayList<>();
        int rn = row.rowNumber;

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
        } else if (existingUrls.contains(url)) {
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
        if (variantStr != null && !variantStr.isBlank()) {
            try {
                VariantType.valueOf(variantStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                errs.add(new BatchProductRowError(rn, "Variant Type",
                        "Invalid variant type: " + variantStr + ". Valid values: " +
                                java.util.Arrays.toString(VariantType.values())));
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

    private Product mapToProduct(ExcelProductRow row,
                                  Map<String, Long> categoryNameToId,
                                  Map<String, Long> brandNameToId) {
        Product product = new Product();

        product.setName(row.get("Name"));
        product.setLocalName(row.get("Local Name"));
        product.setUrl(row.get("URL"));

        Long categoryId = categoryNameToId.get(row.get("Category").toLowerCase());
        product.setCategoryId(categoryId);

        String subCat = row.get("Sub Category");
        if (subCat != null && !subCat.isBlank()) {
            product.setSubCategoryId(categoryNameToId.get(subCat.toLowerCase()));
        }

        String brand = row.get("Brand");
        if (brand != null && !brand.isBlank()) {
            product.setBrandId(brandNameToId.get(brand.toLowerCase()));
        }

        product.setShortDescription(row.get("Short Description"));
        product.setFullDescription(row.get("Full Description"));

        Price price = new Price();
        price.setPrice(new BigDecimal(row.get("Price")));

        String discountStr = row.get("Discount Price");
        if (discountStr != null && !discountStr.isBlank()) {
            price.setDiscountPrice(new BigDecimal(discountStr));
        }

        String variantTypeStr = row.get("Variant Type");
        product.setVariantType(variantTypeStr != null && !variantTypeStr.isBlank()
                ? VariantType.valueOf(variantTypeStr.trim().toUpperCase()) : null);

        String variantValueStr = row.get("Variant Value");
        price.setVariantValue(variantValueStr != null && !variantValueStr.isBlank() ? variantValueStr.trim() : null);

        product.setPrices(new ArrayList<>(List.of(price)));

        product.setInventoryCount(Integer.parseInt(row.get("Inventory Count")));

        String weight = row.get("Weight (grams)");
        product.setWeightGram(weight != null && !weight.isBlank() ? Integer.parseInt(weight) : 0);

        String statusStr = row.get("Status");
        product.setStatus(statusStr != null && !statusStr.isBlank()
                ? ProductStatus.valueOf(statusStr.toUpperCase())
                : ProductStatus.ACTIVE);

        String invStatusStr = row.get("Inventory Status");
        product.setInventoryStatus(invStatusStr != null && !invStatusStr.isBlank()
                ? InventoryStatus.valueOf(invStatusStr.toUpperCase())
                : InventoryStatus.IN_STOCK);

        Map<SpecificationKey, String> specs = new HashMap<>();
        for (SpecificationKey key : SpecificationKey.values()) {
            String val = row.get("Spec:" + key.name());
            if (val != null && !val.isBlank()) {
                specs.put(key, val);
            }
        }
        product.setSpecification(specs);

        return product;
    }

    private String generateCode(Long categoryId) {
        Long seq = jdbcTemplate.queryForObject("SELECT NEXTVAL('product_code_seq')", Long.class);
        return categoryId + "-" + seq;
    }
}
