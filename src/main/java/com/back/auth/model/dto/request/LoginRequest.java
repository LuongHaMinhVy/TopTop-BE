package com.back.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest{
    @NotBlank(message = "{validation.login.email.notblank}")
    private String email;

    @NotBlank(message = "{validation.login.password.notblank}")
    private String password;
}
