package com.jwtcenter.dto.portal;

import java.time.Instant;
import java.util.List;

public record SsoTicketVerifyResponse(
    String appKey,
    String appName,
    String status,
    Instant issuedAt,
    Instant expiresAt,
    SsoUserResponse user
) {
    public record SsoUserResponse(
        Long id,
        String username,
        String displayName,
        String department,
        List<String> roles,
        List<String> permissions
    ) {
    }
}
