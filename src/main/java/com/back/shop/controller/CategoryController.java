package com.back.shop.controller;

import com.back.common.model.dto.response.ApiResponse;
import com.back.shop.model.dto.response.CategoryResponse;
import com.back.shop.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        List<CategoryResponse> data = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .message("Categories retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getFlatCategories() {
        List<CategoryResponse> data = categoryService.getFlatCategories();
        return ResponseEntity.ok(ApiResponse.<List<CategoryResponse>>builder()
                .message("Categories retrieved successfully")
                .data(data)
                .status(200)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
