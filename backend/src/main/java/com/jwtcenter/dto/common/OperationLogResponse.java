package com.jwtcenter.dto.common;

import com.jwtcenter.enums.OperationResult;

import java.time.Instant;

public record OperationLogResponse(
    Long id,
    Long actorId,
    String actorUsername,
    String action,
    String resourceType,
    String resourceId,
    OperationResult result,
    String detail,
    Instant createdAt
) {
}
