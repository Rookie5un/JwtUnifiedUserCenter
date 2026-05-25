package com.jwtcenter.config;

import com.jwtcenter.entity.Department;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.PerformanceRecord;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.PermissionType;
import com.jwtcenter.enums.PerformanceStatus;
import com.jwtcenter.enums.UserStatus;
import com.jwtcenter.repository.DepartmentRepository;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.repository.PerformanceRecordRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(
        JdbcTemplate jdbcTemplate,
        DepartmentRepository departmentRepository,
        PermissionRepository permissionRepository,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PerformanceRecordRepository performanceRecordRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.seed-demo-data:true}") boolean seedDemoData
    ) {
        return args -> {
            migrateUserDepartmentIds(jdbcTemplate);
            syncDepartments(departmentRepository, userRepository, performanceRecordRepository, seedDemoData);
            Map<String, Permission> permissions = syncPermissions(permissionRepository);
            if (!seedDemoData) {
                return;
            }

            if (roleRepository.count() > 0 || userRepository.count() > 0) {
                roleRepository.findByCode("ADMIN").ifPresent(adminRole -> {
                    adminRole.getPermissions().addAll(permissions.values());
                    roleRepository.save(adminRole);
                });
                return;
            }

            Role employeeRole = role("EMPLOYEE", "普通员工", "Self-service employee workspace", Set.of(
                permissions.get("OA_ACCESS"),
                permissions.get("OA_TODO_VIEW"),
                permissions.get("OA_NOTICE_VIEW"),
                permissions.get("WAREHOUSE_ACCESS"),
                permissions.get("WAREHOUSE_STOCK_VIEW"),
                permissions.get("FINANCE_ACCESS"),
                permissions.get("FINANCE_EXPENSE_VIEW"),
                permissions.get("FINANCE_BUDGET_VIEW"),
                permissions.get("PERFORMANCE_ACCESS"),
                permissions.get("PERFORMANCE_VIEW_SELF"),
                permissions.get("PERFORMANCE_CREATE"),
                permissions.get("PERFORMANCE_EDIT_SELF"),
                permissions.get("PERFORMANCE_DELETE_SELF")
            ));
            Role managerRole = role("MANAGER", "部门经理", "Department manager workspace", Set.of(
                permissions.get("OA_ACCESS"),
                permissions.get("OA_TODO_VIEW"),
                permissions.get("OA_PROCESS_CREATE"),
                permissions.get("OA_NOTICE_VIEW"),
                permissions.get("WAREHOUSE_ACCESS"),
                permissions.get("WAREHOUSE_STOCK_VIEW"),
                permissions.get("WAREHOUSE_INBOUND_MANAGE"),
                permissions.get("WAREHOUSE_TRANSFER_APPROVE"),
                permissions.get("FINANCE_ACCESS"),
                permissions.get("FINANCE_EXPENSE_VIEW"),
                permissions.get("FINANCE_PAYMENT_APPROVE"),
                permissions.get("FINANCE_BUDGET_VIEW"),
                permissions.get("PERFORMANCE_ACCESS"),
                permissions.get("PERFORMANCE_VIEW_SELF"),
                permissions.get("PERFORMANCE_CREATE"),
                permissions.get("PERFORMANCE_EDIT_SELF"),
                permissions.get("PERFORMANCE_DELETE_SELF"),
                permissions.get("PERFORMANCE_VIEW_DEPARTMENT"),
                permissions.get("PERFORMANCE_APPROVE")
            ));
            Role adminRole = role("ADMIN", "系统管理员", "Full control workspace", new LinkedHashSet<>(permissions.values()));

            roleRepository.saveAll(List.of(employeeRole, managerRole, adminRole));

            Department eastSales = departmentRepository.findByName("East Sales").orElseThrow();
            Department northSales = departmentRepository.findByName("North Sales").orElseThrow();
            Department southSales = departmentRepository.findByName("South Sales").orElseThrow();

            UserAccount admin = user("admin", "Atlas 管理员", eastSales, "admin@atlas.local", "13800000000", UserStatus.ACTIVE, passwordEncoder.encode("Admin@123"), Set.of(adminRole));
            UserAccount manager = user("manager", "苏南区经理", eastSales, "manager@atlas.local", "13800000001", UserStatus.ACTIVE, passwordEncoder.encode("Manager@123"), Set.of(managerRole));
            UserAccount employee = user("employee", "林初", eastSales, "employee@atlas.local", "13800000002", UserStatus.ACTIVE, passwordEncoder.encode("Employee@123"), Set.of(employeeRole));
            UserAccount employeeTwo = user("employee2", "周航", northSales, "employee2@atlas.local", "13800000003", UserStatus.ACTIVE, passwordEncoder.encode("Employee@123"), Set.of(employeeRole));
            UserAccount employeeThree = user("employee3", "顾屿", southSales, "employee3@atlas.local", "13800000004", UserStatus.ACTIVE, passwordEncoder.encode("Employee@123"), Set.of(employeeRole));

            userRepository.saveAll(List.of(admin, manager, employee, employeeTwo, employeeThree));

            if (performanceRecordRepository.count() == 0) {
                performanceRecordRepository.saveAll(List.of(
                    performance(employee, new BigDecimal("132000.00"), LocalDate.now().minusDays(18), "新签合同", "华东大客户签约", PerformanceStatus.APPROVED, null, manager, Instant.now().minusSeconds(86400 * 14)),
                    performance(employee, new BigDecimal("68000.00"), LocalDate.now().minusDays(10), "续费回款", "年度续费完成", PerformanceStatus.PENDING, null, null, null),
                    performance(employeeTwo, new BigDecimal("54000.00"), LocalDate.now().minusDays(8), "新增渠道", "渠道合作落地", PerformanceStatus.REJECTED, "材料不完整，请补充合同附件", null, null),
                    performance(manager, new BigDecimal("208000.00"), LocalDate.now().minusDays(25), "大项目成交", "重点行业解决方案成交", PerformanceStatus.APPROVED, null, admin, Instant.now().minusSeconds(86400 * 20)),
                    performance(employeeThree, new BigDecimal("96000.00"), LocalDate.now().minusDays(12), "区域拓展", "南区新客户签约", PerformanceStatus.APPROVED, null, admin, Instant.now().minusSeconds(86400 * 9))
                ));
            }
        };
    }

    private Map<String, Permission> syncPermissions(PermissionRepository permissionRepository) {
        Map<String, PermissionSpec> specs = Map.ofEntries(
            Map.entry("OA_ACCESS", new PermissionSpec("进入 OA 系统", "oa", "system:access", PermissionType.MENU, "Access OA collaboration system")),
            Map.entry("OA_TODO_VIEW", new PermissionSpec("查看 OA 待办", "oa", "todo:read", PermissionType.API, "View OA todo items")),
            Map.entry("OA_PROCESS_CREATE", new PermissionSpec("发起 OA 流程", "oa", "process:create", PermissionType.BUTTON, "Create OA workflow requests")),
            Map.entry("OA_NOTICE_VIEW", new PermissionSpec("查看 OA 公告", "oa", "notice:read", PermissionType.API, "View OA notices")),
            Map.entry("WAREHOUSE_ACCESS", new PermissionSpec("进入仓库系统", "warehouse", "system:access", PermissionType.MENU, "Access warehouse management system")),
            Map.entry("WAREHOUSE_STOCK_VIEW", new PermissionSpec("查看库存", "warehouse", "stock:read", PermissionType.API, "View warehouse stock")),
            Map.entry("WAREHOUSE_INBOUND_MANAGE", new PermissionSpec("入库管理", "warehouse", "inbound:manage", PermissionType.BUTTON, "Manage inbound records")),
            Map.entry("WAREHOUSE_TRANSFER_APPROVE", new PermissionSpec("调拨审批", "warehouse", "transfer:approve", PermissionType.BUTTON, "Approve warehouse transfers")),
            Map.entry("FINANCE_ACCESS", new PermissionSpec("进入财务系统", "finance", "system:access", PermissionType.MENU, "Access finance management system")),
            Map.entry("FINANCE_EXPENSE_VIEW", new PermissionSpec("查看报销", "finance", "expense:read", PermissionType.API, "View finance expenses")),
            Map.entry("FINANCE_PAYMENT_APPROVE", new PermissionSpec("付款审批", "finance", "payment:approve", PermissionType.BUTTON, "Approve finance payments")),
            Map.entry("FINANCE_BUDGET_VIEW", new PermissionSpec("查看预算", "finance", "budget:read", PermissionType.API, "View finance budgets")),
            Map.entry("PERFORMANCE_ACCESS", new PermissionSpec("进入业绩审批系统", "performance", "system:access", PermissionType.MENU, "Access performance approval system")),
            Map.entry("PERMISSION_CENTER_ACCESS", new PermissionSpec("进入权限中心", "permission", "system:access", PermissionType.MENU, "Access permission center")),
            Map.entry("PERFORMANCE_VIEW_SELF", new PermissionSpec("查看个人业绩", "performance", "read:self", PermissionType.API, "View personal performance records")),
            Map.entry("PERFORMANCE_CREATE", new PermissionSpec("录入业绩", "performance", "create", PermissionType.BUTTON, "Create performance records")),
            Map.entry("PERFORMANCE_EDIT_SELF", new PermissionSpec("编辑个人业绩", "performance", "update:self", PermissionType.BUTTON, "Edit own performance records")),
            Map.entry("PERFORMANCE_DELETE_SELF", new PermissionSpec("删除个人业绩", "performance", "delete:self", PermissionType.BUTTON, "Delete own performance records")),
            Map.entry("PERFORMANCE_VIEW_DEPARTMENT", new PermissionSpec("查看部门业绩", "performance", "read:department", PermissionType.MENU, "View department performance")),
            Map.entry("PERFORMANCE_APPROVE", new PermissionSpec("审批业绩", "performance", "approve", PermissionType.BUTTON, "Approve or reject performance records")),
            Map.entry("PERFORMANCE_VIEW_GLOBAL", new PermissionSpec("查看全局业绩", "performance", "read:global", PermissionType.MENU, "View global performance")),
            Map.entry("USER_MANAGE", new PermissionSpec("用户管理", "permission", "user:manage", PermissionType.MENU, "Manage users")),
            Map.entry("ROLE_MANAGE", new PermissionSpec("角色管理", "permission", "role:manage", PermissionType.MENU, "Manage roles")),
            Map.entry("PERMISSION_MANAGE", new PermissionSpec("权限管理", "permission", "permission:manage", PermissionType.MENU, "Manage permissions")),
            Map.entry("LOG_VIEW", new PermissionSpec("查看日志", "performance", "audit:read", PermissionType.API, "View operation logs")),
            Map.entry("DOCS_VIEW", new PermissionSpec("接口文档", "performance", "docs:read", PermissionType.MENU, "View API documentation"))
        );

        return specs.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> syncPermission(permissionRepository, entry.getKey(), entry.getValue())
            ));
    }

    private Permission syncPermission(PermissionRepository repository, String code, PermissionSpec spec) {
        Permission permission = repository.findByCode(code).orElseGet(Permission::new);
        permission.setCode(code);
        permission.setName(spec.name());
        permission.setResource(spec.resource());
        permission.setAction(spec.action());
        permission.setType(spec.type());
        permission.setDescription(spec.description());
        return repository.save(permission);
    }

    private void migrateUserDepartmentIds(JdbcTemplate jdbcTemplate) {
        if (!columnExists(jdbcTemplate, "users", "department_id")) {
            return;
        }

        if (columnExists(jdbcTemplate, "users", "department")) {
            jdbcTemplate.update("""
                INSERT INTO departments (created_at, updated_at, description, name)
                SELECT NOW(6), NOW(6), CONCAT(source.name, ' team'), source.name
                FROM (
                    SELECT DISTINCT TRIM(department) AS name
                    FROM users
                    WHERE department IS NOT NULL
                      AND TRIM(department) <> ''
                ) source
                LEFT JOIN departments d ON d.name = source.name
                WHERE d.id IS NULL
                """);
            jdbcTemplate.update("""
                UPDATE users u
                JOIN departments d ON d.name = TRIM(u.department)
                SET u.department_id = d.id
                WHERE u.department_id IS NULL OR u.department_id = 0
                """);
        }

        Integer missingDepartments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE department_id IS NULL OR department_id = 0",
            Integer.class
        );
        if (missingDepartments != null && missingDepartments > 0) {
            jdbcTemplate.update("""
                INSERT INTO departments (created_at, updated_at, description, name)
                SELECT NOW(6), NOW(6), 'Unassigned team', 'Unassigned'
                WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name = 'Unassigned')
                """);
            jdbcTemplate.update("""
                UPDATE users
                SET department_id = (SELECT id FROM departments WHERE name = 'Unassigned')
                WHERE department_id IS NULL OR department_id = 0
                """);
        }

        jdbcTemplate.update("ALTER TABLE users MODIFY department_id BIGINT NOT NULL");
        if (!indexExists(jdbcTemplate, "users", "idx_users_department_id")) {
            jdbcTemplate.update("ALTER TABLE users ADD KEY idx_users_department_id (department_id)");
        }
        if (!foreignKeyExists(jdbcTemplate, "users", "department_id", "departments", "id")) {
            jdbcTemplate.update("""
                ALTER TABLE users
                ADD CONSTRAINT fk_users_department
                FOREIGN KEY (department_id) REFERENCES departments (id)
                """);
        }
        if (columnExists(jdbcTemplate, "users", "department")) {
            jdbcTemplate.update("ALTER TABLE users DROP COLUMN department");
        }
    }

    private boolean columnExists(JdbcTemplate jdbcTemplate, String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean indexExists(JdbcTemplate jdbcTemplate, String tableName, String indexName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND index_name = ?
            """, Integer.class, tableName, indexName);
        return count != null && count > 0;
    }

    private boolean foreignKeyExists(
        JdbcTemplate jdbcTemplate,
        String tableName,
        String columnName,
        String referencedTableName,
        String referencedColumnName
    ) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
              AND referenced_table_schema = DATABASE()
              AND referenced_table_name = ?
              AND referenced_column_name = ?
            """, Integer.class, tableName, columnName, referencedTableName, referencedColumnName);
        return count != null && count > 0;
    }

    private void syncDepartments(
        DepartmentRepository departmentRepository,
        UserRepository userRepository,
        PerformanceRecordRepository performanceRecordRepository,
        boolean seedDemoData
    ) {
        Set<String> departmentNames = new LinkedHashSet<>();
        if (seedDemoData) {
            departmentNames.addAll(List.of(
                "East Sales",
                "North Sales",
                "South Sales",
                "West Sales",
                "Key Accounts",
                "Channel Partners"
            ));
        }
        performanceRecordRepository.findAll().stream()
            .map(PerformanceRecord::getDepartment)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .forEach(departmentNames::add);

        departmentNames.forEach(name -> departmentRepository.findByName(name).orElseGet(() -> {
            Department department = new Department();
            department.setName(name);
            department.setDescription(name + " team");
            return departmentRepository.save(department);
        }));
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
        Department department,
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
        record.setDepartment(owner.getDepartmentName());
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

    private record PermissionSpec(
        String name,
        String resource,
        String action,
        PermissionType type,
        String description
    ) {
    }
}
