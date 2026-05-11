package com.back.common.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ApiResponse<T>{
    private String message;
    private T data;
    private Meta meta;
    private List<ErrorResponse> errors;
    private int status;
    private LocalDateTime timestamp;
}
