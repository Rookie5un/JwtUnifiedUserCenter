package com.jwtcenter.dto.performance;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    String scope,
    BigDecimal totalAmount,
    long totalRecords,
    long pendingCount,
    long approvedCount,
    long rejectedCount,
    List<MonthlyStatResponse> monthly,
    List<RankingResponse> ranking
) {
}
