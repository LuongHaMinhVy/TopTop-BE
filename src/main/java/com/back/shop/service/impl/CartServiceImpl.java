package com.back.shop.service.impl;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.request.AddToCartRequest;
import com.back.shop.model.dto.request.UpdateCartItemRequest;
import com.back.shop.model.dto.response.CartItemResponse;
import com.back.shop.model.dto.response.CartResponse;
import com.back.shop.model.dto.response.CheckoutPreviewResponse;
import com.back.shop.model.dto.response.ShopCheckoutGroup;
import com.back.shop.model.entity.*;
import com.back.shop.model.enums.*;
import com.back.shop.repo.*;
import com.back.shop.service.ICartService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements ICartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMediaRepository productMediaRepository;
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

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    @Override
    @Transactional
    public CartResponse getMyCart() {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());
        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        return mapToResponse(cart, items);
    }

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (product.getIsDeleted() || product.getStatus() != ProductStatus.ACTIVE || product.getModerationStatus() != ProductModerationStatus.APPROVED) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        // Validate shop active
        Shop shop = shopRepository.findById(product.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        if (shop.getStatus() != ShopStatus.ACTIVE || shop.getModerationStatus() != ShopModerationStatus.APPROVED) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = productVariantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
            if (!variant.getProductId().equals(product.getId()) || !"ACTIVE".equals(variant.getStatus())) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
        }

        int requestedQty = request.getQuantity();
        int availableStock = (variant != null) ? variant.getStockQuantity() : product.getStockQuantity();
        if (availableStock < requestedQty) {
            throw new AppException(ErrorCode.BAD_REQUEST); // Insufficient stock
        }

        var existingItemOpt = cartItemRepository.findByCartIdAndProductIdAndVariantId(
                cart.getId(), product.getId(), request.getVariantId());

        if (existingItemOpt.isPresent()) {
            CartItem existing = existingItemOpt.get();
            int newQty = existing.getQuantity() + requestedQty;
            if (availableStock < newQty) {
                throw new AppException(ErrorCode.BAD_REQUEST);
            }
            existing.setQuantity(newQty);
            cartItemRepository.save(existing);
        } else {
            CartItem newItem = CartItem.builder()
                    .cartId(cart.getId())
                    .productId(product.getId())
                    .variantId(request.getVariantId())
                    .quantity(requestedQty)
                    .selected(true)
                    .build();
            cartItemRepository.save(newItem);
        }

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        return mapToResponse(cart, items);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long itemId, UpdateCartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        int availableStock = product.getStockQuantity();
        if (item.getVariantId() != null) {
            ProductVariant variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
            availableStock = variant.getStockQuantity();
        }

        if (availableStock < request.getQuantity()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        return mapToResponse(cart, items);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Long itemId) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        if (!item.getCartId().equals(cart.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        cartItemRepository.delete(item);

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        return mapToResponse(cart, items);
    }

    @Override
    @Transactional
    public CartResponse selectCartItems(List<Long> itemIds, Boolean selected) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());

        List<CartItem> items = cartItemRepository.findAllByCartId(cart.getId());
        for (CartItem item : items) {
            if (itemIds.contains(item.getId())) {
                item.setSelected(selected);
                cartItemRepository.save(item);
            }
        }

        return mapToResponse(cart, items);
    }

    @Override
    public CheckoutPreviewResponse previewCheckout(List<Long> itemIds) {
        User user = getCurrentUser();
        Cart cart = getOrCreateCart(user.getId());
        List<CartItem> allItems = cartItemRepository.findAllByCartId(cart.getId());

        List<CartItem> selectedItems = allItems.stream()
                .filter(item -> itemIds.contains(item.getId()))
                .toList();

        if (selectedItems.isEmpty()) {
            return CheckoutPreviewResponse.builder()
                    .shops(List.of())
                    .subtotalAmount(BigDecimal.ZERO)
                    .shippingFee(BigDecimal.ZERO)
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .currency("VND")
                    .build();
        }

        List<CartItemResponse> responses = selectedItems.stream()
                .map(this::mapItem)
                .toList();

        // Group by Shop
        Map<Long, List<CartItemResponse>> shopGroups = responses.stream()
                .collect(Collectors.groupingBy(item -> {
                    Product p = productRepository.findById(item.getProductId()).orElseThrow();
                    return p.getShopId();
                }));

        List<ShopCheckoutGroup> checkoutGroups = new ArrayList<>();
        BigDecimal totalSubtotal = BigDecimal.ZERO;

        for (var entry : shopGroups.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItemResponse> groupItems = entry.getValue();

            Shop shop = shopRepository.findById(shopId).orElseThrow();

            BigDecimal groupSubtotal = groupItems.stream()
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            totalSubtotal = totalSubtotal.add(groupSubtotal);

            checkoutGroups.add(ShopCheckoutGroup.builder()
                    .shopId(shopId)
                    .shopName(shop.getName())
                    .items(groupItems)
                    .subtotal(groupSubtotal)
                    .build());
        }

        // Apply a basic fixed shipping fee per shop (e.g. 30000 VND)
        BigDecimal totalShipping = BigDecimal.valueOf(30000L * checkoutGroups.size());
        BigDecimal totalAmount = totalSubtotal.add(totalShipping);

        return CheckoutPreviewResponse.builder()
                .shops(checkoutGroups)
                .subtotalAmount(totalSubtotal)
                .shippingFee(totalShipping)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .currency("VND")
                .build();
    }

    private CartResponse mapToResponse(Cart cart, List<CartItem> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::mapItem)
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .items(itemResponses)
                .build();
    }

    private CartItemResponse mapItem(CartItem item) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        String mediaUrl = null;
        List<ProductMedia> mediaList = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(product.getId());
        if (!mediaList.isEmpty()) {
            mediaUrl = mediaList.get(0).getUrl();
        }

        ProductVariant variant = null;
        if (item.getVariantId() != null) {
            variant = productVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        }

        BigDecimal price = (variant != null) ? variant.getPrice() : product.getBasePrice();
        String varName = (variant != null) ? variant.getName() : null;
        int stock = (variant != null) ? variant.getStockQuantity() : product.getStockQuantity();

        boolean isAvailable = !product.getIsDeleted()
                && product.getStatus() == ProductStatus.ACTIVE
                && product.getModerationStatus() == ProductModerationStatus.APPROVED
                && stock >= item.getQuantity();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productTitle(product.getTitle())
                .productImageUrl(mediaUrl)
                .variantId(item.getVariantId())
                .variantName(varName)
                .price(price)
                .quantity(item.getQuantity())
                .selected(item.getSelected())
                .stockQuantity(stock)
                .isAvailable(isAvailable)
                .build();
    }
}
