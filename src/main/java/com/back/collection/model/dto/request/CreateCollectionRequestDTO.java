package com.back.collection.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCollectionRequestDTO {

    @NotBlank(message = "Collection name is required")
    @Size(max = 80, message = "Collection name must not exceed 80 characters")
    private String name;

    @Size(max = 240, message = "Collection description must not exceed 240 characters")
    private String description;

    private Boolean isPublic;
}
