package com.jwtcenter.dto.token;

import jakarta.validation.constraints.NotBlank;

public record TokenInspectRequest(@NotBlank String token) {
}
