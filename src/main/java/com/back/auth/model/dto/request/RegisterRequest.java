package com.back.auth.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "{validation.register.username.notblank}")
    @Size(min = 2, max = 24, message = "{validation.register.username.size}")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "{validation.register.username.pattern}"
    )
    private String username;

    @NotBlank(message = "{validation.register.email.notblank}")
    @Email(message = "{validation.register.email.format}")
    private String email;

    @NotBlank(message = "{validation.register.password.notblank}")
    @Size(min = 8, max = 20, message = "{validation.register.password.size}")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,20}$",
            message = "{validation.register.password.pattern}"
    )
    private String password;

    private String dateOfBirth;
}