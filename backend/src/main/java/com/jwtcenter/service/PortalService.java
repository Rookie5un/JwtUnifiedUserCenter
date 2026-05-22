package com.jwtcenter.service;

import com.jwtcenter.dto.portal.PortalAppResponse;
import com.jwtcenter.dto.portal.PortalMetricResponse;
import com.jwtcenter.dto.portal.SsoAuthorizeResponse;
import com.jwtcenter.dto.portal.SsoTicketVerifyResponse;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class PortalService {

    private static final List<PortalAppDefinition> APPS = List.of(
        new PortalAppDefinition(
            "oa",
            "OA 协同办公系统",
            "OA",
            "统一待办、通知公告、流程申请和个人办公入口。",
            "OA",
            "portal-app",
            Map.of("appKey", "oa"),
            List.of(),
            List.of("OA_ACCESS"),
            "demo",
            "协同办公",
            "#2f6f73",
            List.of(
                new PortalMetricResponse("今日待办", "8"),
                new PortalMetricResponse("流程处理中", "14")
            ),
            "/systems/oa"
        ),
        new PortalAppDefinition(
            "permission",
            "权限中心",
            "权限",
            "管理用户、角色、权限和组织部门，支撑统一授权。",
            "权",
            "standalone-system",
            Map.of("appKey", "permission"),
            List.of(),
            List.of("PERMISSION_CENTER_ACCESS"),
            "online",
            "统一用户中心",
            "#6b5b95",
            List.of(
                new PortalMetricResponse("角色策略", "3"),
                new PortalMetricResponse("权限点", "11")
            ),
            "/systems/permission"
        ),
        new PortalAppDefinition(
            "warehouse",
            "仓库管理系统",
            "仓库",
            "查看库存、入库出库、调拨单和异常预警示例。",
            "仓",
            "portal-app",
            Map.of("appKey", "warehouse"),
            List.of(),
            List.of("WAREHOUSE_ACCESS"),
            "demo",
            "供应链",
            "#4d6b42",
            List.of(
                new PortalMetricResponse("库存品类", "126"),
                new PortalMetricResponse("待处理单据", "9")
            ),
            "/systems/warehouse"
        ),
        new PortalAppDefinition(
            "finance",
            "财务管理系统",
            "财务",
            "模拟费用报销、付款申请、预算执行和凭证查询。",
            "财",
            "portal-app",
            Map.of("appKey", "finance"),
            List.of(),
            List.of("FINANCE_ACCESS"),
            "demo",
            "财务运营",
            "#9a6a2f",
            List.of(
                new PortalMetricResponse("待审批报销", "6"),
                new PortalMetricResponse("本月预算", "86%")
            ),
            "/systems/finance"
        ),
        new PortalAppDefinition(
            "performance",
            "业绩审批系统",
            "业绩",
            "复用现有业绩录入、审批、统计看板，并收拢审计与接口文档入口。",
            "绩",
            "portal-app",
            Map.of("appKey", "performance"),
            List.of(),
            List.of("PERFORMANCE_ACCESS"),
            "online",
            "业务审批",
            "#b4683c",
            List.of(
                new PortalMetricResponse("个人台账", "启用"),
                new PortalMetricResponse("支撑入口", "日志/接口")
            ),
            "/systems/performance"
        )
    );

    private final Map<String, SsoTicket> tickets = new ConcurrentHashMap<>();
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public PortalService(AccessService accessService, OperationLogService operationLogService) {
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    public List<PortalAppResponse> visibleApps() {
        UserAccount user = accessService.currentUser();
        return APPS.stream()
            .filter(app -> canAccess(user, app))
            .map(PortalService::toResponse)
            .toList();
    }

    public SsoAuthorizeResponse authorize(String appKey) {
        UserAccount user = accessService.currentUser();
        PortalAppDefinition app = APPS.stream()
            .filter(item -> item.key().equals(appKey))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APP_NOT_FOUND", "Business system does not exist."));

        if (!canAccess(user, app)) {
            operationLogService.log(user, "APP_ACCESS", "PORTAL_APP", app.key(), OperationResult.FAILURE, "Access denied for " + app.name() + ".");
            throw new ApiException(HttpStatus.FORBIDDEN, "APP_ACCESS_DENIED", "You do not have access to this business system.");
        }

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(5, ChronoUnit.MINUTES);
        String ticket = "SSO-" + app.key().toUpperCase() + "-" + UUID.randomUUID();
        tickets.put(ticket, new SsoTicket(ticket, app.key(), user.getId(), issuedAt, expiresAt));
        cleanupExpiredTickets(issuedAt);
        operationLogService.log(user, "APP_ACCESS", "PORTAL_APP", app.key(), OperationResult.SUCCESS, "Authorized access to " + app.name() + ".");
        String entryPath = app.targetPath() + "?ticket=" + ticket;
        return new SsoAuthorizeResponse(app.key(), app.name(), ticket, app.targetPath(), entryPath, issuedAt, expiresAt);
    }

    public SsoTicketVerifyResponse verifyTicket(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SSO_TICKET_REQUIRED", "SSO ticket is required.");
        }

        UserAccount currentUser = accessService.currentUser();
        SsoTicket existing = tickets.get(ticket);
        Instant now = Instant.now();
        if (existing == null || existing.expiresAt().isBefore(now)) {
            tickets.remove(ticket);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SSO_TICKET_INVALID", "SSO ticket is invalid or expired.");
        }
        if (!existing.userId().equals(currentUser.getId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SSO_TICKET_USER_MISMATCH", "SSO ticket does not belong to the current user.");
        }

        PortalAppDefinition app = APPS.stream()
            .filter(item -> item.key().equals(existing.appKey()))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APP_NOT_FOUND", "Business system does not exist."));

        if (!canAccess(currentUser, app)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "APP_ACCESS_DENIED", "You do not have access to this business system.");
        }

        return new SsoTicketVerifyResponse(
            app.key(),
            app.name(),
            "VERIFIED",
            existing.issuedAt(),
            existing.expiresAt(),
            new SsoTicketVerifyResponse.SsoUserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getDisplayName(),
                currentUser.getDepartment(),
                currentUser.getRoles().stream().map(role -> role.getCode()).sorted().toList(),
                currentUser.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .map(permission -> permission.getCode())
                    .distinct()
                    .sorted()
                    .toList()
            )
        );
    }

    private void cleanupExpiredTickets(Instant now) {
        new ArrayList<>(tickets.entrySet()).forEach(entry -> {
            if (entry.getValue().expiresAt().isBefore(now)) {
                tickets.remove(entry.getKey());
            }
        });
    }

    private boolean canAccess(UserAccount user, PortalAppDefinition app) {
        boolean roleMatched = app.requiredRoles().isEmpty() ||
            app.requiredRoles().stream().anyMatch(role -> accessService.hasRole(user, role));
        boolean permissionMatched = app.requiredPermissions().isEmpty() ||
            app.requiredPermissions().stream().allMatch(permission -> accessService.hasPermission(user, permission));
        return roleMatched && permissionMatched;
    }

    private static PortalAppResponse toResponse(PortalAppDefinition app) {
        return new PortalAppResponse(
            app.key(),
            app.name(),
            app.shortName(),
            app.description(),
            app.iconLabel(),
            app.routeName(),
            app.routeParams(),
            app.requiredRoles(),
            app.requiredPermissions(),
            app.status(),
            app.category(),
            app.accent(),
            app.metrics()
        );
    }

    private record PortalAppDefinition(
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
        List<PortalMetricResponse> metrics,
        String targetPath
    ) {
    }

    private record SsoTicket(
        String ticket,
        String appKey,
        Long userId,
        Instant issuedAt,
        Instant expiresAt
    ) {
    }
}
