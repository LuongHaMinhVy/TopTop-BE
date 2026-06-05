package com.back.shop.service;

import com.back.shop.model.dto.request.CreateShopRequest;
import com.back.shop.model.dto.request.UpdateShopRequest;
import com.back.shop.model.dto.response.ShopResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IShopService {
    ShopResponse createShop(CreateShopRequest request);
    ShopResponse updateShop(UpdateShopRequest request);
    ShopResponse getMyShop();
    ShopResponse getShopBySlug(String slug);
    ShopResponse getShopById(Long id);
    Page<ShopResponse> getShopsForAdmin(String status, String moderationStatus, Pageable pageable);
    ShopResponse moderateShop(Long id, String moderationStatus);
    ShopResponse suspendShop(Long id);
    ShopResponse unsuspendShop(Long id);
}
