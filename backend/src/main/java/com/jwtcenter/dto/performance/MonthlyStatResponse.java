package com.jwtcenter.dto.performance;

import java.math.BigDecimal;

public record MonthlyStatResponse(
    String month,
    BigDecimal totalAmount
) {
}
