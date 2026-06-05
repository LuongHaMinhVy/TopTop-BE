package com.back.shop.service;

import com.back.shop.model.dto.response.ProductResponse;

import java.util.List;

public interface IProductLinkService {
    void linkProductsToVideo(Long videoId, List<Long> productIds);
    List<ProductResponse> getProductsByVideo(Long videoId);
    void pinProductToLivestream(Long livestreamId, Long productId);
    void unpinProductFromLivestream(Long livestreamId, Long productId);
    List<ProductResponse> getProductsByLivestream(Long livestreamId);
}
