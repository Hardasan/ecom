package com.ecommerce.application.service.product;

import com.ecommerce.application.api.dto.product.CreateProductRequestDto;
import com.ecommerce.application.api.dto.product.GetProductResponseDto;
import com.ecommerce.application.api.dto.product.SearchProductRequestDto;
import com.ecommerce.application.api.dto.product.SearchProductResponseDto;
import com.ecommerce.application.api.dto.product.SpecialSaleProductListResponseDto;
import com.ecommerce.application.api.dto.product.enumeration.ImageType;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import com.ecommerce.persistence.entity.Product;
import com.ecommerce.persistence.entity.ProductImage;
import com.ecommerce.persistence.entity.ProductOtherImage;
import com.ecommerce.persistence.entity.enumeration.ProductStatus;
import com.ecommerce.persistence.repository.BrandRepository;
import com.ecommerce.persistence.repository.CategoryRepository;
import com.ecommerce.persistence.repository.ProductRepository;
import com.ecommerce.persistence.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecommerce.application.util.SecurityUtil.isAdmin;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductReviewRepository productReviewRepository;
    private final ProductMapper productMapper;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public GetProductResponseDto create(CreateProductRequestDto requestDto, MultipartFile mainImageFile, String altText) {

        validateUrlForCreate(requestDto.getUrl());

        validateProductRequestDto(requestDto);

        var product = new Product();
        productMapper.apply(requestDto, product);
        product.setPrices(ProductMapper.mapPrices(requestDto));
        product.setCode(generateCode(requestDto.getCategoryId()));

        if (mainImageFile != null) {
            var image = new ProductImage();
            image.setAltText(altText);
            image.setImageData(toBase64(mainImageFile));
            product.setMainImage(image);
        }

        return productMapper.toResponseDto(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public GetProductResponseDto getById(Long id) {
        Product product = findProductOrThrow(id);
        if (!isAdmin() && product.getStatus() != ProductStatus.ACTIVE) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND);
        }
        return productMapper.toResponseDto(product);
    }

    @Transactional(readOnly = true)
    public Page<SearchProductResponseDto> search(SearchProductRequestDto searchDto, Pageable pageable) {
        Page<SearchProductResponseDto> page = productRepository
                .findAll(ProductSpecifications.build(searchDto, isAdmin()), pageable)
                .map(productMapper::toSummaryDto);
        enrichRatings(page.getContent());
        return page;
    }

    /**
     * Home "فروش ویژه" strip. Selection rule can change later; for now return up to 5
     * active in-stock products (newest id first).
     */
    @Transactional(readOnly = true)
    public SpecialSaleProductListResponseDto getSpecialSale() {
        var products = productRepository
                .findTop5ByStatusAndInventoryCountGreaterThanOrderByIdDesc(ProductStatus.ACTIVE, 0)
                .stream()
                .map(productMapper::toSummaryDto)
                .toList();
        enrichRatings(products);
        return new SpecialSaleProductListResponseDto(products);
    }

    /**
     * Fill in averageRating + ratingCount on a page/list of product summaries from a single grouped
     * query over PUBLISHED reviews. Products with no published reviews get ratingCount 0 (no badge).
     */
    private void enrichRatings(List<SearchProductResponseDto> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        var ids = items.stream().map(SearchProductResponseDto::getId).toList();
        Map<Long, Double> avg = new HashMap<>();
        Map<Long, Long> count = new HashMap<>();
        for (Object[] row : productReviewRepository.aggregatePublishedByProductIds(ids)) {
            Long productId = (Long) row[0];
            double average = row[1] == null ? 0d : ((Number) row[1]).doubleValue();
            avg.put(productId, BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue());
            count.put(productId, ((Number) row[2]).longValue());
        }
        for (SearchProductResponseDto dto : items) {
            dto.setRatingCount(count.getOrDefault(dto.getId(), 0L));
            dto.setAverageRating(avg.getOrDefault(dto.getId(), 0d));
        }
    }

    @Transactional
    public GetProductResponseDto update(Long id, CreateProductRequestDto requestDto) {

        var product = findProductOrThrow(id);

        validateUrlForUpdate(requestDto.getUrl(), id);

        validateProductRequestDto(requestDto);

        productMapper.apply(requestDto, product);
        product.setPrices(ProductMapper.mapPrices(requestDto));

        return productMapper.toResponseDto(productRepository.save(product));
    }

    @Transactional
    public GetProductResponseDto uploadImage(Long productId, ImageType type, MultipartFile file, String altText) {

        var product = findProductOrThrow(productId);

        if (type == ImageType.MAIN) {
            var image = new ProductImage();
            image.setAltText(altText);
            image.setImageData(toBase64(file));
            product.setMainImage(image);
        } else {
            var image = new ProductOtherImage();
            image.setProduct(product);
            image.setAltText(altText);
            image.setImageData(toBase64(file));
            product.getOtherImages().add(image);
        }

        return productMapper.toResponseDto(productRepository.save(product));
    }

    @Transactional
    public void removeImage(Long productId, ImageType type, Long imageId) {
        if (type == ImageType.OTHER && imageId == null) {
            throw new EcommerceException(ECOMErrorType.VALIDATION_ERROR);
        }
        var product = findProductOrThrow(productId);
        if (type == ImageType.MAIN) {
            product.setMainImage(null);
        } else {
            boolean removed = product.getOtherImages().removeIf(img -> img.getId().equals(imageId));
            if (!removed) {
                throw new EcommerceException(ECOMErrorType.VALIDATION_ERROR);
            }
        }
        productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findProductOrThrow(id));
    }

    private String generateCode(Long categoryId) {
        Long seq = jdbcTemplate.queryForObject("SELECT NEXTVAL('product_code_seq')", Long.class);
        return categoryId + "-" + seq;
    }

    private String toBase64(MultipartFile file) {
        try {
            return Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException e) {
            throw new EcommerceException(ECOMErrorType.FILE_UPLOAD_FAILED);
        }
    }

    private void validateUrlForCreate(String url) {
        if (productRepository.existsByUrl(url)) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_URL_ALREADY_EXISTS);
        }
    }

    private void validateUrlForUpdate(String url, Long id) {
        if (productRepository.existsByUrlAndIdNot(url, id)) {
            throw new EcommerceException(ECOMErrorType.PRODUCT_URL_ALREADY_EXISTS);
        }
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EcommerceException(ECOMErrorType.PRODUCT_NOT_FOUND));
    }

    private void validateProductRequestDto(CreateProductRequestDto requestDto) {
        if (!categoryRepository.existsById(requestDto.getCategoryId())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND);
        }
        if (requestDto.getSubCategoryId() != null && !categoryRepository.existsById(requestDto.getSubCategoryId())) {
            throw new EcommerceException(ECOMErrorType.CATEGORY_NOT_FOUND);
        }
        if (requestDto.getBrandId() != null && !brandRepository.existsById(requestDto.getBrandId())) {
            throw new EcommerceException(ECOMErrorType.BRAND_NOT_FOUND);
        }

        // A non-null variantValue on any Price row implies the product's variantType must also be set.
        for (var price : requestDto.getPrices()) {
            if (price.getVariantValue() != null && requestDto.getVariantType() == null) {
                throw new EcommerceException(ECOMErrorType.VALIDATION_ERROR);
            }
        }
    }
}