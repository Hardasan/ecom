package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.BatchProductUploadResult;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Brand;
import com.ecommerce.persistence.entity.Category;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.repository.BrandRepository;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductBatchService_UTest {

    @Mock
    private ProductExcelParserService parserService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private ProductBatchService service;

    @BeforeEach
    void setUp() {
        service = new ProductBatchService(parserService, productRepository, categoryRepository,
                brandRepository, jdbcTemplate);
    }

    @Test
    void valid_rows_are_saved_and_counted_as_success() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row1 = new ExcelProductRow(2);
        row1.put("Name", "Product 1");
        row1.put("URL", "product-1");
        row1.put("Category", "Electronics");
        row1.put("Price", "100");
        row1.put("Inventory Count", "10");

        var row2 = new ExcelProductRow(3);
        row2.put("Name", "Product 2");
        row2.put("URL", "product-2");
        row2.put("Category", "Electronics");
        row2.put("Price", "200");
        row2.put("Inventory Count", "5");

        when(parserService.parse(any(), any())).thenReturn(List.of(row1, row2));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L, 2L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(2, result.getTotalRows());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertNull(result.getErrors());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        List<Product> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals("Product 1", saved.get(0).getName());
        assertEquals("product-1", saved.get(0).getUrl());
        assertEquals(Long.valueOf(1L), saved.get(0).getCategoryId());
    }

    @Test
    void invalid_rows_are_skipped_and_reported_as_errors() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row1 = new ExcelProductRow(2);
        row1.put("Name", "Valid Product");
        row1.put("URL", "valid-product");
        row1.put("Category", "Electronics");
        row1.put("Price", "100");
        row1.put("Inventory Count", "10");

        var row2 = new ExcelProductRow(3);
        row2.put("Name", ""); // Missing name
        row2.put("URL", "bad-product");
        row2.put("Category", "Electronics");
        row2.put("Price", "invalid"); // Bad price
        row2.put("Inventory Count", "5");

        var row3 = new ExcelProductRow(4);
        // Missing URL
        row3.put("Name", "No URL Product");
        row3.put("Category", "Electronics");
        row3.put("Price", "150");
        row3.put("Inventory Count", "3");

        when(parserService.parse(any(), any())).thenReturn(List.of(row1, row2, row3));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(3, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
        assertNotNull(result.getErrors());
        assertEquals(3, result.getErrors().size());
    }

    @Test
    void duplicate_url_in_file_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row1 = new ExcelProductRow(2);
        row1.put("Name", "First");
        row1.put("URL", "duplicate-url");
        row1.put("Category", "Electronics");
        row1.put("Price", "100");
        row1.put("Inventory Count", "10");

        var row2 = new ExcelProductRow(3);
        row2.put("Name", "Second");
        row2.put("URL", "duplicate-url");
        row2.put("Category", "Electronics");
        row2.put("Price", "200");
        row2.put("Inventory Count", "5");

        when(parserService.parse(any(), any())).thenReturn(List.of(row1, row2));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("URL", result.getErrors().get(0).getField());
        assertTrue(result.getErrors().get(0).getMessage().contains("Duplicate"));
    }

    @Test
    void existing_url_in_database_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "existing-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of("existing-url"));

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("already exists"));
        verify(productRepository, never()).saveAll(anyList());
    }

    @Test
    void category_not_found_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "NonExistent");
        row.put("Price", "100");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Category not found"));
    }

    @Test
    void brand_not_found_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Brand", "NonExistentBrand");
        row.put("Price", "100");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        var knownBrand = new Brand();
        knownBrand.setId(1L);
        knownBrand.setName("KnownBrand");
        when(brandRepository.findAll()).thenReturn(List.of(knownBrand));
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Brand not found"));
    }

    @Test
    void negative_inventory_count_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "-5");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains(">= 0"));
    }

    @Test
    void negative_price_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "-1");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("greater than zero"));
    }

    @Test
    void zero_price_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "0");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("greater than zero"));
    }

    @Test
    void invalid_variant_type_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");
        row.put("Variant Value", "#FF0000");
        row.put("Variant Type", "INVALID_TYPE");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.getMessage().contains("Invalid variant type")));
    }

    @Test
    void invalid_status_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");
        row.put("Status", "INVALID_STATUS");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Invalid status"));
    }

    @Test
    void defaults_are_applied_for_optional_fields() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Minimal Product");
        row.put("URL", "minimal");
        row.put("Category", "Electronics");
        row.put("Price", "50");
        row.put("Inventory Count", "1");
        // No status, no inventory status, no variant type, no weight

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(42L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(1, result.getSuccessCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        Product saved = captor.getValue().get(0);
        assertEquals(0, saved.getWeightGram());
        assertEquals(com.ecommerce.persistence.entity.enumeration.ProductStatus.ACTIVE, saved.getStatus());
        assertEquals(com.ecommerce.persistence.entity.enumeration.InventoryStatus.IN_STOCK, saved.getInventoryStatus());
        assertEquals("Minimal Product", saved.getName());
        assertTrue(saved.getCode().startsWith("1-"));
    }

    @Test
    void elapsed_time_is_present() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L);

        BatchProductUploadResult result = service.processUpload(file);

        assertTrue(result.getElapsedTimeMs() >= 0);
    }

    @Test
    void variant_value_without_variant_type_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");
        row.put("Variant Value", "#FF0000");
        // No Variant Type

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Variant Value requires Variant Type"));
    }

    @Test
    void variant_type_without_variant_value_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");
        row.put("Variant Type", "COLOR");
        // No Variant Value

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Variant Value is required"));
    }

    @Test
    void invalid_variant_value_for_type_is_rejected() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "100");
        row.put("Inventory Count", "10");
        row.put("Variant Type", "COLOR");
        row.put("Variant Value", "EXTRA_LARGE"); // Not a valid COLOR value

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("not valid for variant type COLOR"));
    }

    @Test
    void valid_variant_product_is_accepted() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Variant Product");
        row.put("URL", "variant-product");
        row.put("Category", "Electronics");
        row.put("Price", "150");
        row.put("Inventory Count", "5");
        row.put("Variant Type", "COLOR");
        row.put("Variant Value", "#0000FF");

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        Product saved = captor.getValue().get(0);
        assertEquals(com.ecommerce.persistence.entity.enumeration.VariantType.COLOR,
                saved.getVariantType());
        assertEquals("#0000FF", saved.getPrices().get(0).getVariantValue());
    }

    @Test
    void lowercase_variant_value_is_normalized_to_uppercase() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        var row = new ExcelProductRow(2);
        row.put("Name", "Product");
        row.put("URL", "product-url");
        row.put("Category", "Electronics");
        row.put("Price", "150");
        row.put("Inventory Count", "5");
        row.put("Variant Type", "COLOR");
        row.put("Variant Value", "#ff0000"); // lowercase hex

        when(parserService.parse(any(), any())).thenReturn(List.of(row));

        var electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(electronics));
        when(brandRepository.findAll()).thenReturn(List.of());
        when(productRepository.findAllUrls()).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class))).thenReturn(1L);

        BatchProductUploadResult result = service.processUpload(file);

        assertEquals(1, result.getSuccessCount());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        Product saved = captor.getValue().get(0);
        assertEquals("#FF0000", saved.getPrices().get(0).getVariantValue());
    }

    @Test
    void empty_file_from_parser_propagates_exception() throws Exception {
        var file = new MockMultipartFile("file", "test.xlsx", "application/octet-stream", new byte[0]);

        when(parserService.parse(any(), any()))
                .thenThrow(new EcommerceException(ECOMErrorType.EXCEL_EMPTY_FILE));

        var ex = org.junit.jupiter.api.Assertions.assertThrows(EcommerceException.class,
                () -> service.processUpload(file));
        assertEquals(ECOMErrorType.EXCEL_EMPTY_FILE, ex.getEcomErrorType());
    }
}
