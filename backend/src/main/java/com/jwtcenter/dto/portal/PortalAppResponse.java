package com.jwtcenter.dto.portal;

import java.util.List;
import java.util.Map;

public record PortalAppResponse(
    String key,
    String name,
    String shortName,
    String description,
    String iconLabel,
    String routeName,
    Map<String, String> routeParams,
    List<String> requiredRoles,
    List<String> requiredPermissions,
    String status,
    String category,
    String accent,
    List<PortalMetricResponse> metrics
) {
}
