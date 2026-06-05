package com.back.shop.service.impl;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.request.CreateShopRequest;
import com.back.shop.model.dto.request.UpdateShopRequest;
import com.back.shop.model.dto.response.ShopResponse;
import com.back.shop.model.entity.Shop;
import com.back.shop.model.enums.ShopModerationStatus;
import com.back.shop.model.enums.ShopStatus;
import com.back.shop.repo.ShopRepository;
import com.back.shop.service.IShopService;
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

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ShopServiceImpl implements IShopService {

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
    public ShopResponse createShop(CreateShopRequest request) {
        User currentUser = getCurrentUser();
        if (shopRepository.existsByOwnerId(currentUser.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST); // Already has a shop
        }

        String slug = toSlug(request.getName());
        if (slug.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        if (shopRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        Shop shop = Shop.builder()
                .ownerId(currentUser.getId())
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .avatarUrl(request.getAvatarUrl())
                .bannerUrl(request.getBannerUrl())
                .status(ShopStatus.DRAFT)
                .moderationStatus(ShopModerationStatus.PENDING)
                .build();

        Shop saved = shopRepository.save(shop);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ShopResponse updateShop(UpdateShopRequest request) {
        User currentUser = getCurrentUser();
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST)); // Doesn't have a shop

        if (request.getName() != null && !request.getName().isBlank()) {
            shop.setName(request.getName());
            String slug = toSlug(request.getName());
            if (!slug.equals(shop.getSlug())) {
                if (shopRepository.existsBySlug(slug)) {
                    slug = slug + "-" + System.currentTimeMillis() % 10000;
                }
                shop.setSlug(slug);
            }
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getAvatarUrl() != null) {
            shop.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBannerUrl() != null) {
            shop.setBannerUrl(request.getBannerUrl());
        }

        Shop saved = shopRepository.save(shop);
        return mapToResponse(saved);
    }

    @Override
    public ShopResponse getMyShop() {
        User currentUser = getCurrentUser();
        Shop shop = shopRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        return mapToResponse(shop);
    }

    @Override
    public ShopResponse getShopBySlug(String slug) {
        Shop shop = shopRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        return mapToResponse(shop);
    }

    @Override
    public ShopResponse getShopById(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        return mapToResponse(shop);
    }

    @Override
    public Page<ShopResponse> getShopsForAdmin(String status, String moderationStatus, Pageable pageable) {
        // Simple admin fetching - Spring Security checks roles at controller level
        if (status != null && moderationStatus != null) {
            return shopRepository.findAllByStatusAndModerationStatus(
                    ShopStatus.valueOf(status), ShopModerationStatus.valueOf(moderationStatus), pageable)
                    .map(this::mapToResponse);
        }
        return shopRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ShopResponse moderateShop(Long id, String moderationStatus) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        ShopModerationStatus modStatus = ShopModerationStatus.valueOf(moderationStatus);
        shop.setModerationStatus(modStatus);
        if (modStatus == ShopModerationStatus.APPROVED) {
            shop.setStatus(ShopStatus.ACTIVE);
        } else if (modStatus == ShopModerationStatus.REJECTED) {
            shop.setStatus(ShopStatus.CLOSED);
        }

        Shop saved = shopRepository.save(shop);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ShopResponse suspendShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        shop.setStatus(ShopStatus.SUSPENDED);
        return mapToResponse(shopRepository.save(shop));
    }

    @Override
    @Transactional
    public ShopResponse unsuspendShop(Long id) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (shop.getModerationStatus() == ShopModerationStatus.APPROVED) {
            shop.setStatus(ShopStatus.ACTIVE);
        } else {
            shop.setStatus(ShopStatus.DRAFT);
        }
        return mapToResponse(shopRepository.save(shop));
    }

    private ShopResponse mapToResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .ownerId(shop.getOwnerId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .avatarUrl(shop.getAvatarUrl())
                .bannerUrl(shop.getBannerUrl())
                .status(shop.getStatus().name())
                .moderationStatus(shop.getModerationStatus().name())
                .build();
    }
}
