package com.back.shop.model.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Integer sortOrder;
    private List<CategoryResponse> children;
}
