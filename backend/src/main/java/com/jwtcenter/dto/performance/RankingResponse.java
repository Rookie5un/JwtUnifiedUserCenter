package com.jwtcenter.dto.performance;

import java.math.BigDecimal;

public record RankingResponse(
    String name,
    String department,
    BigDecimal totalAmount
) {
}
