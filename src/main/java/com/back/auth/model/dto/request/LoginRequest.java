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
    @NotBlank(message = "Vui lòng nhập email hoặc mật khẩu")
    private String email;

    @NotBlank(message = "Vui lòng nhập email hoặc mật khẩu")
    private String password;
}
