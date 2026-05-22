package com.jwtcenter.dto.portal;

import java.time.Instant;

public record SsoAuthorizeResponse(
    String appKey,
    String appName,
    String accessTicket,
    String targetPath,
    String entryPath,
    Instant issuedAt,
    Instant expiresAt
) {
}
