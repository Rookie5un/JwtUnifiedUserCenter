package com.jwtcenter.dto.performance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PerformanceRecordRequest(
    @NotNull @DecimalMin("0.01") BigDecimal amount,
    @NotNull LocalDate occurredOn,
    @NotBlank @Size(max = 80) String type,
    @Size(max = 400) String note
) {
}
