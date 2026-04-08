package com.jwtcenter.config;

import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.PermissionType;
import com.jwtcenter.enums.PerformanceStatus;
import com.jwtcenter.enums.UserStatus;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.repository.PerformanceRecordRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
        PermissionRepository permissionRepository,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PerformanceRecordRepository performanceRecordRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.seed-demo-data:true}") boolean seedDemoData
    ) {
        return args -> {
            if (!seedDemoData) {
                return;
            }

            if (roleRepository.count() > 0 || userRepository.count() > 0) {
                return;
            }

            Map<String, Permission> permissions = Map.ofEntries(
                Map.entry("PERFORMANCE_VIEW_SELF", permission(permissionRepository, "PERFORMANCE_VIEW_SELF", "查看个人业绩", "performance", "read:self", PermissionType.API, "View personal performance records")),
                Map.entry("PERFORMANCE_CREATE", permission(permissionRepository, "PERFORMANCE_CREATE", "录入业绩", "performance", "create", PermissionType.BUTTON, "Create performance records")),
                Map.entry("PERFORMANCE_EDIT_SELF", permission(permissionRepository, "PERFORMANCE_EDIT_SELF", "编辑个人业绩", "performance", "update:self", PermissionType.BUTTON, "Edit own performance records")),
                Map.entry("PERFORMANCE_DELETE_SELF", permission(permissionRepository, "PERFORMANCE_DELETE_SELF", "删除个人业绩", "performance", "delete:self", PermissionType.BUTTON, "Delete own performance records")),
                Map.entry("PERFORMANCE_VIEW_DEPARTMENT", permission(permissionRepository, "PERFORMANCE_VIEW_DEPARTMENT", "查看部门业绩", "performance", "read:department", PermissionType.MENU, "View department performance")),
                Map.entry("PERFORMANCE_APPROVE", permission(permissionRepository, "PERFORMANCE_APPROVE", "审批业绩", "performance", "approve", PermissionType.BUTTON, "Approve or reject performance records")),
                Map.entry("PERFORMANCE_VIEW_GLOBAL", permission(permissionRepository, "PERFORMANCE_VIEW_GLOBAL", "查看全局业绩", "performance", "read:global", PermissionType.MENU, "View global performance")),
                Map.entry("USER_MANAGE", permission(permissionRepository, "USER_MANAGE", "用户管理", "user", "manage", PermissionType.MENU, "Manage users")),
                Map.entry("ROLE_MANAGE", permission(permissionRepository, "ROLE_MANAGE", "角色管理", "role", "manage", PermissionType.MENU, "Manage roles")),
                Map.entry("PERMISSION_MANAGE", permission(permissionRepository, "PERMISSION_MANAGE", "权限管理", "permission", "manage", PermissionType.MENU, "Manage permissions")),
                Map.entry("LOG_VIEW", permission(permissionRepository, "LOG_VIEW", "查看日志", "logs", "read", PermissionType.API, "View operation logs"))
            );

            Role employeeRole = role("EMPLOYEE", "普通员工", "Self-service employee workspace", Set.of(
                permissions.get("PERFORMANCE_VIEW_SELF"),
                permissions.get("PERFORMANCE_CREATE"),
                permissions.get("PERFORMANCE_EDIT_SELF"),
                permissions.get("PERFORMANCE_DELETE_SELF")
            ));
            Role managerRole = role("MANAGER", "部门经理", "Department manager workspace", Set.of(
                permissions.get("PERFORMANCE_VIEW_SELF"),
                permissions.get("PERFORMANCE_CREATE"),
                permissions.get("PERFORMANCE_EDIT_SELF"),
                permissions.get("PERFORMANCE_DELETE_SELF"),
                permissions.get("PERFORMANCE_VIEW_DEPARTMENT"),
                permissions.get("PERFORMANCE_APPROVE")
            ));
            Role adminRole = role("ADMIN", "系统管理员", "Full control workspace", new LinkedHashSet<>(permissions.values()));

            roleRepository.saveAll(List.of(employeeRole, managerRole, adminRole));

            UserAccount admin = user("admin", "Atlas 管理员", "Platform", "admin@atlas.local", "13800000000", UserStatus.ACTIVE, passwordEncoder.encode("Admin@123"), Set.of(adminRole));
            UserAccount manager = user("manager", "苏南区经理", "East Sales", "manager@atlas.local", "13800000001", UserStatus.ACTIVE, passwordEncoder.encode("Manager@123"), Set.of(managerRole));
            UserAccount employee = user("employee", "林初", "East Sales", "employee@atlas.local", "13800000002", UserStatus.ACTIVE, passwordEncoder.encode("Employee@123"), Set.of(employeeRole));
            UserAccount employeeTwo = user("employee2", "周航", "East Sales", "employee2@atlas.local", "13800000003", UserStatus.ACTIVE, passwordEncoder.encode("Employee@123"), Set.of(employeeRole));

            userRepository.saveAll(List.of(admin, manager, employee, employeeTwo));

            if (performanceRecordRepository.count() == 0) {
                performanceRecordRepository.saveAll(List.of(
                    performance(employee, new BigDecimal("132000.00"), LocalDate.now().minusDays(18), "新签合同", "华东大客户签约", PerformanceStatus.APPROVED, null, manager, Instant.now().minusSeconds(86400 * 14)),
                    performance(employee, new BigDecimal("68000.00"), LocalDate.now().minusDays(10), "续费回款", "年度续费完成", PerformanceStatus.PENDING, null, null, null),
                    performance(employeeTwo, new BigDecimal("54000.00"), LocalDate.now().minusDays(8), "新增渠道", "渠道合作落地", PerformanceStatus.REJECTED, "材料不完整，请补充合同附件", null, null),
                    performance(manager, new BigDecimal("208000.00"), LocalDate.now().minusDays(25), "大项目成交", "重点行业解决方案成交", PerformanceStatus.APPROVED, null, admin, Instant.now().minusSeconds(86400 * 20))
                ));
            }
        };
    }

    private Permission permission(
        PermissionRepository repository,
        String code,
        String name,
        String resource,
        String action,
        PermissionType type,
        String description
    ) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setType(type);
        permission.setDescription(description);
        return repository.save(permission);
    }

    private Role role(String code, String name, String description, Set<Permission> permissions) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setPermissions(permissions);
        return role;
    }

    private UserAccount user(
        String username,
        String displayName,
        String department,
        String email,
        String phone,
        UserStatus status,
        String passwordHash,
        Set<Role> roles
    ) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setDepartment(department);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(status);
        user.setPasswordHash(passwordHash);
        user.setRoles(roles);
        return user;
    }

    private PerformanceRecord performance(
        UserAccount owner,
        BigDecimal amount,
        LocalDate occurredOn,
        String type,
        String note,
        PerformanceStatus status,
        String rejectedReason,
        UserAccount approvedBy,
        Instant approvedAt
    ) {
        PerformanceRecord record = new PerformanceRecord();
        record.setOwner(owner);
        record.setDepartment(owner.getDepartment());
        record.setAmount(amount);
        record.setOccurredOn(occurredOn);
        record.setType(type);
        record.setNote(note);
        record.setStatus(status);
        record.setRejectedReason(rejectedReason);
        record.setApprovedBy(approvedBy);
        record.setApprovedAt(approvedAt);
        return record;
    }
}
