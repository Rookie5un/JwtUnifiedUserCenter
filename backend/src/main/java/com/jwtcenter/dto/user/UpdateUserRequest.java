package com.jwtcenter.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(max = 80) String displayName,
    @Size(max = 32) String phone,
    @Email @Size(max = 120) String email,
    @NotBlank @Size(max = 80) String department
) {
}
