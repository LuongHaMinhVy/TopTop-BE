package com.back.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "{validation.reset.token.notblank}")
    private String token;

    @NotBlank(message = "{validation.reset.password.notblank}")
    private String newPassword;
}