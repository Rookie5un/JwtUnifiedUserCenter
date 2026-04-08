package com.jwtcenter.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @NotBlank @Size(min = 8, max = 60) String password,
    @NotBlank @Size(max = 80) String displayName,
    @NotBlank @Size(max = 80) String department,
    @Email @Size(max = 120) String email,
    @Size(max = 32) String phone
) {
}
