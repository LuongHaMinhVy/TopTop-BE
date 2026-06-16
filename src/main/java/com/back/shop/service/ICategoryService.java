package com.back.shop.service;

import com.back.shop.model.dto.response.CategoryResponse;

import java.util.List;

public interface ICategoryService {
    List<CategoryResponse> getCategoryTree();
    List<CategoryResponse> getFlatCategories();
}
