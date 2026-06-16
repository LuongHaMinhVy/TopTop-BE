package com.back.user.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequestDTO {

    @NotBlank(message = "Username cannot be empty")
    @Size(min = 2, max = 24, message = "Username must be between 2 and 24 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._]+$", message = "Username can only contain letters, numbers, underscores and dots")
    private String username;

    @NotBlank(message = "Nickname cannot be empty")
    @Size(min = 1, max = 30, message = "Name must be between 1 and 30 characters")
    private String nickname;

    @Size(max = 80, message = "Bio must not exceed 80 characters")
    private String bio;

    private String avatarUrl;
}
