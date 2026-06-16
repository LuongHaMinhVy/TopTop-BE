package com.back.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountStatusActionRequestDTO {
    @NotBlank
    private String password;

    @NotBlank
    @Pattern(regexp = "DEACTIVATE|DELETE", flags = Pattern.Flag.CASE_INSENSITIVE)
    private String action;
}
