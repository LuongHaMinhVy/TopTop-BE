package com.back.auth.model.dto.request;

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
public class OAuth2OnboardRequest {

    @NotBlank(message = "{validation.register.username.notblank}")
    @Size(min = 2, max = 24, message = "{validation.register.username.size}")
    @Pattern(
            regexp = "^[a-zA-Z0-9._]+$",
            message = "{validation.register.username.pattern}"
    )
    private String username;

    @NotBlank(message = "Date of birth is required")
    private String dateOfBirth;
}
