package com.back.shop.service.impl;

import com.back.shop.model.dto.response.CategoryResponse;
import com.back.shop.model.entity.Category;
import com.back.shop.repo.CategoryRepository;
import com.back.shop.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryResponse> getCategoryTree() {
        List<Category> allActive = categoryRepository.findByIsActiveTrueOrderBySortOrderAsc();

        Map<Long, List<CategoryResponse>> byParent = allActive.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(
                        Category::getParentId,
                        Collectors.mapping(this::toResponse, Collectors.toList())
                ));

        return allActive.stream()
                .filter(c -> c.getParentId() == null)
                .map(root -> {
                    CategoryResponse resp = toResponse(root);
                    resp.setChildren(byParent.getOrDefault(root.getId(), List.of()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getFlatCategories() {
        return categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParentId())
                .sortOrder(category.getSortOrder())
                .children(List.of())
                .build();
    }
}
