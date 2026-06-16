package com.back.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequestDTO {
    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
