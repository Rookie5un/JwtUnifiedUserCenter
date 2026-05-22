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
            List.of(),
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
            List.of("USER_MANAGE", "ROLE_MANAGE", "PERMISSION_MANAGE"),
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
            List.of(),
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
            List.of(),
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
            "复用现有业绩录入、审批、统计看板作为真实业务示例。",
            "绩",
            "portal-app",
            Map.of("appKey", "performance"),
            List.of(),
            List.of("PERFORMANCE_VIEW_SELF"),
            "online",
            "业务审批",
            "#b4683c",
            List.of(
                new PortalMetricResponse("个人台账", "启用"),
                new PortalMetricResponse("审批流", "JWT")
            ),
            "/systems/performance"
        ),
        new PortalAppDefinition(
            "logs",
            "操作日志系统",
            "日志",
            "审计登录、授权、业务操作和系统访问记录。",
            "志",
            "standalone-system",
            Map.of("appKey", "logs"),
            List.of(),
            List.of("LOG_VIEW"),
            "online",
            "安全审计",
            "#3d5872",
            List.of(
                new PortalMetricResponse("审计范围", "全局"),
                new PortalMetricResponse("登录事件", "记录")
            ),
            "/systems/logs"
        ),
        new PortalAppDefinition(
            "docs",
            "接口文档系统",
            "接口",
            "查看 RESTful API、JWT 鉴权和后端接口说明。",
            "API",
            "standalone-system",
            Map.of("appKey", "docs"),
            List.of("ADMIN"),
            List.of(),
            "online",
            "开发支撑",
            "#4c6171",
            List.of(
                new PortalMetricResponse("接口规范", "REST"),
                new PortalMetricResponse("认证方式", "Bearer")
            ),
            "/systems/docs"
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

        SsoTicket existing = tickets.get(ticket);
        Instant now = Instant.now();
        if (existing == null || existing.expiresAt().isBefore(now)) {
            tickets.remove(ticket);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SSO_TICKET_INVALID", "SSO ticket is invalid or expired.");
        }

        UserAccount user = accessService.userById(existing.userId());
        PortalAppDefinition app = APPS.stream()
            .filter(item -> item.key().equals(existing.appKey()))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "APP_NOT_FOUND", "Business system does not exist."));

        if (!canAccess(user, app)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "APP_ACCESS_DENIED", "You do not have access to this business system.");
        }

        return new SsoTicketVerifyResponse(
            app.key(),
            app.name(),
            "VERIFIED",
            existing.issuedAt(),
            existing.expiresAt(),
            new SsoTicketVerifyResponse.SsoUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartment(),
                user.getRoles().stream().map(role -> role.getCode()).sorted().toList(),
                user.getRoles().stream()
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
