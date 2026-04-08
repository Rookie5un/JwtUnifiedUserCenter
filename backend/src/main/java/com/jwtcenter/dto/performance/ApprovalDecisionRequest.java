package com.jwtcenter.dto.performance;

import jakarta.validation.constraints.Size;

public record ApprovalDecisionRequest(@Size(max = 255) String reason) {
}
