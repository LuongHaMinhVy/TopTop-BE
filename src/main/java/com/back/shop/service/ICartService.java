package com.back.shop.service;

import com.back.shop.model.dto.request.AddToCartRequest;
import com.back.shop.model.dto.request.UpdateCartItemRequest;
import com.back.shop.model.dto.response.CartResponse;
import com.back.shop.model.dto.response.CheckoutPreviewResponse;

import java.util.List;

public interface ICartService {
    CartResponse getMyCart();
    CartResponse addToCart(AddToCartRequest request);
    CartResponse updateCartItem(Long itemId, UpdateCartItemRequest request);
    CartResponse removeCartItem(Long itemId);
    CartResponse selectCartItems(List<Long> itemIds, Boolean selected);
    CheckoutPreviewResponse previewCheckout(List<Long> itemIds);
}
