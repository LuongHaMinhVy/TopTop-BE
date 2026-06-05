package com.back.shop.service.impl;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.request.CreateProductRequest;
import com.back.shop.model.dto.request.UpdateProductRequest;
import com.back.shop.model.dto.response.ProductMediaResponse;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.model.dto.response.ProductReviewResponse;
import com.back.shop.model.dto.response.ProductVariantResponse;
import com.back.shop.model.entity.*;
import com.back.shop.model.enums.*;
import com.back.shop.repo.*;
import com.back.shop.service.IProductService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopRepository shopRepository;
    private final ProductReviewRepository productReviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final IUserRepo userRepo;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Shop getOwnShop() {
        User currentUser = getCurrentUser();
        return shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN)); // Only sellers with shops can manage products
    }

    private String toSlug(String input) {
        if (input == null || input.isEmpty()) return "";
        String nowhitespace = Pattern.compile("\\s+").matcher(input.trim()).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String unicodeBlockPattern = "\\p{InCombiningDiacriticalMarks}+";
        String slug = Pattern.compile(unicodeBlockPattern).matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = Pattern.compile("[^a-z0-9-]").matcher(slug).replaceAll("");
        slug = Pattern.compile("-+").matcher(slug).replaceAll("-");
        return slug;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Shop shop = getOwnShop();
        String slug = toSlug(request.getTitle());
        if (slug.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (productRepository.existsByShopIdAndSlug(shop.getId(), slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        Product product = Product.builder()
                .shopId(shop.getId())
                .title(request.getTitle())
                .slug(slug)
                .description(request.getDescription())
                .categoryId(request.getCategoryId())
                .basePrice(request.getBasePrice())
                .currency(request.getCurrency() != null ? request.getCurrency() : "VND")
                .stockQuantity(request.getStockQuantity())
                .status(ProductStatus.DRAFT)
                .moderationStatus(ProductModerationStatus.PENDING)
                .isDeleted(false)
                .build();

        Product savedProduct = productRepository.save(product);

        List<ProductMedia> savedMedia = new ArrayList<>();
        if (request.getMedia() != null) {
            for (var m : request.getMedia()) {
                ProductMedia pm = ProductMedia.builder()
                        .productId(savedProduct.getId())
                        .url(m.getUrl())
                        .storageKey(m.getStorageKey())
                        .mediaType(ProductMediaType.valueOf(m.getMediaType().toUpperCase()))
                        .sortOrder(m.getSortOrder())
                        .build();
                savedMedia.add(productMediaRepository.save(pm));
            }
        }

        List<ProductVariant> savedVariants = new ArrayList<>();
        if (request.getVariants() != null) {
            for (var v : request.getVariants()) {
                ProductVariant pv = ProductVariant.builder()
                        .productId(savedProduct.getId())
                        .name(v.getName())
                        .sku(v.getSku())
                        .optionValues(v.getOptionValues())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .status("ACTIVE")
                        .build();
                savedVariants.add(productVariantRepository.save(pv));
            }
        }

        return mapToResponse(savedProduct, savedMedia, savedVariants);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Shop shop = getOwnShop();
        Product product = productRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (product.getIsDeleted()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            product.setTitle(request.getTitle());
            String slug = toSlug(request.getTitle());
            if (!slug.equals(product.getSlug())) {
                if (productRepository.existsByShopIdAndSlug(shop.getId(), slug)) {
                    slug = slug + "-" + System.currentTimeMillis() % 10000;
                }
                product.setSlug(slug);
            }
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategoryId() != null) {
            product.setCategoryId(request.getCategoryId());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }

        Product savedProduct = productRepository.save(product);

        if (request.getMedia() != null) {
            productMediaRepository.deleteAllByProductId(savedProduct.getId());
            List<ProductMedia> savedMedia = new ArrayList<>();
            for (var m : request.getMedia()) {
                ProductMedia pm = ProductMedia.builder()
                        .productId(savedProduct.getId())
                        .url(m.getUrl())
                        .storageKey(m.getStorageKey())
                        .mediaType(ProductMediaType.valueOf(m.getMediaType().toUpperCase()))
                        .sortOrder(m.getSortOrder())
                        .build();
                savedMedia.add(productMediaRepository.save(pm));
            }
        }

        if (request.getVariants() != null) {
            productVariantRepository.deleteAllByProductId(savedProduct.getId());
            List<ProductVariant> savedVariants = new ArrayList<>();
            for (var v : request.getVariants()) {
                ProductVariant pv = ProductVariant.builder()
                        .productId(savedProduct.getId())
                        .name(v.getName())
                        .sku(v.getSku())
                        .optionValues(v.getOptionValues())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .status("ACTIVE")
                        .build();
                savedVariants.add(productVariantRepository.save(pv));
            }
        }

        List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(savedProduct.getId());
        List<ProductVariant> variants = productVariantRepository.findAllByProductId(savedProduct.getId());
        return mapToResponse(savedProduct, media, variants);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Shop shop = getOwnShop();
        Product product = productRepository.findByIdAndShopId(id, shop.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        product.setIsDeleted(true);
        productRepository.save(product);
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (product.getIsDeleted()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findAllByProductId(product.getId());
        return mapToResponse(product, media, variants);
    }

    @Override
    public ProductResponse getProductBySlug(Long shopId, String slug) {
        // Find public product
        Product product = productRepository.findAllByShopId(shopId, Pageable.unpaged())
                .stream()
                .filter(p -> p.getSlug().equals(slug) && !p.getIsDeleted())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findAllByProductId(product.getId());
        return mapToResponse(product, media, variants);
    }

    @Override
    public Page<ProductResponse> getMyProducts(Pageable pageable) {
        Shop shop = getOwnShop();
        return productRepository.findAllByShopIdAndIsDeleted(shop.getId(), false, pageable)
                .map(p -> {
                    List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
                    List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
                    return mapToResponse(p, media, variants);
                });
    }

    @Override
    public Page<ProductResponse> getProductsForAdmin(String status, String moderationStatus, Pageable pageable) {
        ProductStatus productStatus = status != null && !status.isBlank() ? ProductStatus.valueOf(status) : null;
        ProductModerationStatus modStatus = moderationStatus != null && !moderationStatus.isBlank()
                ? ProductModerationStatus.valueOf(moderationStatus)
                : null;

        Page<Product> products;
        if (productStatus != null && modStatus != null) {
            products = productRepository.findAllByStatusAndModerationStatus(productStatus, modStatus, pageable);
        } else if (productStatus != null) {
            products = productRepository.findAllByStatus(productStatus, pageable);
        } else if (modStatus != null) {
            products = productRepository.findAllByModerationStatus(modStatus, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(p -> {
            List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
            List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
            return mapToResponse(p, media, variants);
        });
    }

    @Override
    public Page<ProductResponse> getPublicProducts(String keyword, Long categoryId, Pageable pageable) {
        return productRepository.findPublicProducts(ProductStatus.ACTIVE, ProductModerationStatus.APPROVED, keyword, categoryId, pageable)
                .map(p -> {
                    List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
                    List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
                    return mapToResponse(p, media, variants);
                });
    }

    @Override
    public Page<ProductResponse> getPublicProductsByShop(String shopSlug, Pageable pageable) {
        return productRepository.findPublicProductsByShopSlug(shopSlug, pageable)
                .map(p -> {
                    List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
                    List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
                    return mapToResponse(p, media, variants);
                });
    }

    @Override
    @Transactional
    public ProductResponse moderateProduct(Long id, String moderationStatus) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        ProductModerationStatus modStatus = ProductModerationStatus.valueOf(moderationStatus);
        product.setModerationStatus(modStatus);
        if (modStatus == ProductModerationStatus.APPROVED) {
            product.setStatus(ProductStatus.ACTIVE);
        } else if (modStatus == ProductModerationStatus.REJECTED) {
            product.setStatus(ProductStatus.HIDDEN);
        } else if (modStatus == ProductModerationStatus.NEED_REVIEW) {
            product.setStatus(ProductStatus.HIDDEN);
        }

        Product savedProduct = productRepository.save(product);
        List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(savedProduct.getId());
        List<ProductVariant> variants = productVariantRepository.findAllByProductId(savedProduct.getId());
        return mapToResponse(savedProduct, media, variants);
    }

    @Override
    @Transactional
    public ProductReviewResponse createReview(com.back.shop.model.dto.request.CreateReviewRequest request) {
        User currentUser = getCurrentUser();
        OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        Order order = orderRepository.findById(item.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (!order.getBuyerId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.COMPLETED && order.getStatus() != OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (productReviewRepository.existsByOrderItemId(request.getOrderItemId())) {
            throw new AppException(ErrorCode.BAD_REQUEST); // Already reviewed
        }

        ProductReview review = ProductReview.builder()
                .productId(item.getProductId())
                .orderItemId(item.getId())
                .userId(currentUser.getId())
                .rating(request.getRating())
                .content(request.getContent())
                .status("ACTIVE")
                .build();

        ProductReview saved = productReviewRepository.save(review);

        // Update product ratings
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        long count = product.getRatingCount() + 1;
        BigDecimal sum = product.getRatingAvg().multiply(BigDecimal.valueOf(product.getRatingCount()))
                .add(BigDecimal.valueOf(request.getRating()));
        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        product.setRatingCount(count);
        product.setRatingAvg(avg);
        productRepository.save(product);

        return ProductReviewResponse.builder()
                .id(saved.getId())
                .productId(saved.getProductId())
                .orderItemId(saved.getOrderItemId())
                .userId(saved.getUserId())
                .username(currentUser.getUsername())
                .userAvatarUrl(currentUser.getAvatarUrl())
                .rating(saved.getRating())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return productReviewRepository.findAllByProductId(productId, pageable)
                .map(r -> {
                    User u = userRepo.findById(r.getUserId()).orElse(null);
                    return ProductReviewResponse.builder()
                            .id(r.getId())
                            .productId(r.getProductId())
                            .orderItemId(r.getOrderItemId())
                            .userId(r.getUserId())
                            .username(u != null ? u.getUsername() : "Anonymous")
                            .userAvatarUrl(u != null ? u.getAvatarUrl() : null)
                            .rating(r.getRating())
                            .content(r.getContent())
                            .createdAt(r.getCreatedAt())
                            .build();
                });
    }

    private ProductResponse mapToResponse(Product product, List<ProductMedia> media, List<ProductVariant> variants) {
        List<ProductMediaResponse> mediaRes = media.stream()
                .map(m -> ProductMediaResponse.builder()
                        .id(m.getId())
                        .url(m.getUrl())
                        .mediaType(m.getMediaType().name())
                        .sortOrder(m.getSortOrder())
                        .build())
                .toList();

        List<ProductVariantResponse> varRes = variants.stream()
                .map(v -> ProductVariantResponse.builder()
                        .id(v.getId())
                        .sku(v.getSku())
                        .name(v.getName())
                        .optionValues(v.getOptionValues())
                        .price(v.getPrice())
                        .stockQuantity(v.getStockQuantity())
                        .status(v.getStatus())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .shopId(product.getShopId())
                .title(product.getTitle())
                .slug(product.getSlug())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .basePrice(product.getBasePrice())
                .currency(product.getCurrency())
                .stockQuantity(product.getStockQuantity())
                .soldCount(product.getSoldCount())
                .ratingAvg(product.getRatingAvg())
                .ratingCount(product.getRatingCount())
                .status(product.getStatus().name())
                .moderationStatus(product.getModerationStatus().name())
                .media(mediaRes)
                .variants(varRes)
                .build();
    }
}
