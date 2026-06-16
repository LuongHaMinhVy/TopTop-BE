package com.back.common.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Meta {
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;

    public static <T> Meta from(Page<T> page) {
        return Meta.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }
}