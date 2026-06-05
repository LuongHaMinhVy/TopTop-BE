package com.back.shop.service.impl;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.response.ProductMediaResponse;
import com.back.shop.model.dto.response.ProductResponse;
import com.back.shop.model.dto.response.ProductVariantResponse;
import com.back.shop.model.entity.*;
import com.back.shop.model.enums.*;
import com.back.shop.repo.*;
import com.back.shop.service.IProductLinkService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductLinkServiceImpl implements IProductLinkService {

    private final VideoProductLinkRepository videoProductLinkRepository;
    private final LivestreamProductPinRepository livestreamProductPinRepository;
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ShopRepository shopRepository;
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
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN));
    }

    @Override
    @Transactional
    public void linkProductsToVideo(Long videoId, List<Long> productIds) {
        Shop shop = getOwnShop();
        // Clear previous video links
        var existingLinks = videoProductLinkRepository.findAllByVideoIdOrderBySortOrderAsc(videoId);
        videoProductLinkRepository.deleteAll(existingLinks);

        int order = 0;
        for (Long pid : productIds) {
            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

            if (!product.getShopId().equals(shop.getId())) {
                throw new AppException(ErrorCode.FORBIDDEN); // Can only link own products
            }

            VideoProductLink link = VideoProductLink.builder()
                    .videoId(videoId)
                    .productId(pid)
                    .sellerId(shop.getId())
                    .pinned(false)
                    .sortOrder(order++)
                    .build();
            videoProductLinkRepository.save(link);
        }
    }

    @Override
    public List<ProductResponse> getProductsByVideo(Long videoId) {
        List<VideoProductLink> links = videoProductLinkRepository.findAllByVideoIdOrderBySortOrderAsc(videoId);
        List<ProductResponse> responses = new ArrayList<>();
        for (VideoProductLink link : links) {
            Optional<Product> prodOpt = productRepository.findById(link.getProductId());
            if (prodOpt.isPresent()) {
                Product p = prodOpt.get();
                // Filter by active + approved + non-deleted
                if (!p.getIsDeleted() && p.getStatus() == ProductStatus.ACTIVE && p.getModerationStatus() == ProductModerationStatus.APPROVED) {
                    List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
                    List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
                    responses.add(mapToResponse(p, media, variants));
                }
            }
        }
        return responses;
    }

    @Override
    @Transactional
    public void pinProductToLivestream(Long livestreamId, Long productId) {
        Shop shop = getOwnShop();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (!product.getShopId().equals(shop.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        Optional<LivestreamProductPin> pinOpt = livestreamProductPinRepository.findByLivestreamIdAndProductId(livestreamId, productId);
        if (pinOpt.isPresent()) {
            LivestreamProductPin pin = pinOpt.get();
            pin.setActive(true);
            livestreamProductPinRepository.save(pin);
        } else {
            LivestreamProductPin pin = LivestreamProductPin.builder()
                    .livestreamId(livestreamId)
                    .productId(productId)
                    .pinnedBy(shop.getOwnerId())
                    .active(true)
                    .sortOrder(0)
                    .build();
            livestreamProductPinRepository.save(pin);
        }
    }

    @Override
    @Transactional
    public void unpinProductFromLivestream(Long livestreamId, Long productId) {
        Shop shop = getOwnShop();
        LivestreamProductPin pin = livestreamProductPinRepository.findByLivestreamIdAndProductId(livestreamId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (!pin.getPinnedBy().equals(shop.getOwnerId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        pin.setActive(false);
        livestreamProductPinRepository.save(pin);
    }

    @Override
    public List<ProductResponse> getProductsByLivestream(Long livestreamId) {
        List<LivestreamProductPin> pins = livestreamProductPinRepository.findAllByLivestreamIdAndActiveOrderBySortOrderAsc(livestreamId, true);
        List<ProductResponse> responses = new ArrayList<>();
        for (LivestreamProductPin pin : pins) {
            Optional<Product> prodOpt = productRepository.findById(pin.getProductId());
            if (prodOpt.isPresent()) {
                Product p = prodOpt.get();
                if (!p.getIsDeleted() && p.getStatus() == ProductStatus.ACTIVE && p.getModerationStatus() == ProductModerationStatus.APPROVED) {
                    List<ProductMedia> media = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(p.getId());
                    List<ProductVariant> variants = productVariantRepository.findAllByProductId(p.getId());
                    responses.add(mapToResponse(p, media, variants));
                }
            }
        }
        return responses;
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
