package com.back.user.model.dto.request;

import com.back.user.model.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdateUserStatusRequestDTO {
    @NotNull
    private UserStatus status;

    @Size(max = 500)
    private String reason;
}
