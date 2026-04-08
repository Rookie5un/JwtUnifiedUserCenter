package com.jwtcenter.dto.performance;

import com.jwtcenter.enums.PerformanceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PerformanceRecordResponse(
    Long id,
    Long ownerId,
    String ownerName,
    String department,
    BigDecimal amount,
    LocalDate occurredOn,
    String type,
    String note,
    PerformanceStatus status,
    String rejectedReason,
    Instant createdAt,
    Instant approvedAt,
    String approvedBy
) {
}
