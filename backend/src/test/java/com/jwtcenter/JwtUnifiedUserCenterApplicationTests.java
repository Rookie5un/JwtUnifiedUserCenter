package com.jwtcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.repository.DepartmentRepository;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.repository.PerformanceRecordRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.enums.PerformanceStatus;
import com.jwtcenter.security.PermissionCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtUnifiedUserCenterApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PerformanceRecordRepository performanceRecordRepository;

    @Test
    void registerShouldCreateUserWithEncryptedPassword() throws Exception {
        long eastSalesId = departmentId("East Sales");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new.employee",
                      "password": "Passw0rd!",
                      "displayName": "新员工",
                      "departmentId": %d,
                      "email": "new.employee@atlas.local",
                      "phone": "13800000099"
                    }
                    """.formatted(eastSalesId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("new.employee"))
            .andExpect(jsonPath("$.data.departmentId").value((int) eastSalesId))
            .andExpect(jsonPath("$.data.department").value("East Sales"));

        UserAccount saved = userRepository.findByUsername("new.employee").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Passw0rd!");
        assertThat(saved.getPasswordHash()).startsWith("$2");
        assertThat(saved.getDepartment().getId()).isEqualTo(eastSalesId);
    }

    @Test
    void adminCanLoadUsersAfterLogin() throws Exception {
        String accessToken = login("admin", "Admin@123");

        mockMvc.perform(get("/users")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNumber());
    }

    @Test
    void publicDepartmentCatalogShouldLoadWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/departments/public"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").isNumber())
            .andExpect(jsonPath("$.data[0].name").isNotEmpty());
    }

    @Test
    void adminCanUpdateUsernameForManagedUser() throws Exception {
        String accessToken = login("admin", "Admin@123");
        UserAccount employee = userRepository.findByUsername("employee").orElseThrow();
        String renamedUsername = "employee.renamed." + System.nanoTime();
        long eastSalesId = departmentId("East Sales");

        mockMvc.perform(put("/users/" + employee.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "displayName": "林初",
                      "departmentId": %d,
                      "email": "employee@atlas.local",
                      "phone": "13800000002"
                    }
                    """.formatted(renamedUsername, eastSalesId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value(renamedUsername));

        UserAccount updatedUser = userRepository.findById(employee.getId()).orElseThrow();
        assertThat(updatedUser.getUsername()).isEqualTo(renamedUsername);
        assertThat(updatedUser.getDepartment().getId()).isEqualTo(eastSalesId);
    }

    @Test
    void employeeSubmissionCanBeApprovedByManagerAndLockedAfterApproval() throws Exception {
        String employeeToken = login("employee", "Employee@123");
        MvcResult creationResult = mockMvc.perform(post("/performance/records")
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": 91000,
                      "occurredOn": "%s",
                      "type": "项目拓展",
                      "note": "测试审批流"
                    }
                    """.formatted(LocalDate.now())))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode createdBody = objectMapper.readTree(creationResult.getResponse().getContentAsString());
        long recordId = createdBody.path("data").path("id").asLong();

        String managerToken = login("manager", "Manager@123");
        mockMvc.perform(get("/performance/approvals/pending")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.id==" + recordId + ")]").exists());

        mockMvc.perform(post("/performance/approvals/" + recordId + "/approve")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(put("/performance/records/" + recordId)
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": %s,
                      "occurredOn": "%s",
                      "type": "项目拓展",
                      "note": "审批后尝试修改"
                    }
                    """.formatted(BigDecimal.valueOf(95000), LocalDate.now())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("RECORD_LOCKED"));
    }

    @Test
    void dashboardTotalAmountShouldOnlyCountApprovedRecords() throws Exception {
        String accessToken = login("admin", "Admin@123");
        BigDecimal expectedTotal = performanceRecordRepository.findAll()
            .stream()
            .filter(record -> record.getStatus() == PerformanceStatus.APPROVED)
            .map(record -> record.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long expectedApprovedCount = performanceRecordRepository.findAll()
            .stream()
            .filter(record -> record.getStatus() == PerformanceStatus.APPROVED)
            .count();
        long expectedPendingCount = performanceRecordRepository.findAll()
            .stream()
            .filter(record -> record.getStatus() == PerformanceStatus.PENDING)
            .count();
        long expectedRejectedCount = performanceRecordRepository.findAll()
            .stream()
            .filter(record -> record.getStatus() == PerformanceStatus.REJECTED)
            .count();

        MvcResult result = mockMvc.perform(get("/performance/dashboard/global")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.approvedCount").value((int) expectedApprovedCount))
            .andExpect(jsonPath("$.data.pendingCount").value((int) expectedPendingCount))
            .andExpect(jsonPath("$.data.rejectedCount").value((int) expectedRejectedCount))
            .andReturn();

        BigDecimal actualTotal = objectMapper.readTree(result.getResponse().getContentAsString())
            .path("data")
            .path("totalAmount")
            .decimalValue();
        assertThat(actualTotal).isEqualByComparingTo(expectedTotal);
    }

    @Test
    void adminCanLogicallyDeleteUserAndRevokeTheirAccess() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String username = "deleted.user." + System.nanoTime();
        long eastSalesId = departmentId("East Sales");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "Passw0rd!",
                      "displayName": "待删除用户",
                      "departmentId": %d,
                      "email": "%s@atlas.local",
                      "phone": "13800000999"
                    }
                    """.formatted(username, eastSalesId, username)))
            .andExpect(status().isOk());

        UserAccount createdUser = userRepository.findByUsername(username).orElseThrow();
        String deletedUserToken = login(username, "Passw0rd!");

        mockMvc.perform(delete("/users/" + createdUser.getId())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        UserAccount deletedUser = userRepository.findById(createdUser.getId()).orElseThrow();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(userRepository.findByIdAndDeletedAtIsNull(createdUser.getId())).isEmpty();

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "Passw0rd!"
                    }
                    """.formatted(username)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + deletedUserToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminCanClearRolePermissions() throws Exception {
        String accessToken = login("admin", "Admin@123");
        Role employeeRole = roleRepository.findByCode("EMPLOYEE").orElseThrow();

        mockMvc.perform(post("/roles/" + employeeRole.getId() + "/permissions")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "permissionIds": []
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.permissions.length()").value(0));

        Role updatedRole = roleRepository.findById(employeeRole.getId()).orElseThrow();
        assertThat(updatedRole.getPermissions()).isEmpty();
    }

    @Test
    void adminCanClearUserRoles() throws Exception {
        String accessToken = login("admin", "Admin@123");
        UserAccount employee = userRepository.findByUsername("employee").orElseThrow();

        mockMvc.perform(post("/users/" + employee.getId() + "/roles")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleIds": []
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.roles.length()").value(0))
            .andExpect(jsonPath("$.data.permissions.length()").value(0));

        UserAccount updatedUser = userRepository.findById(employee.getId()).orElseThrow();
        assertThat(updatedUser.getRoles()).isEmpty();
    }

    @Test
    void updatingRolePermissionsShouldImmediatelyAffectBackendAuthorization() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String roleCode = "LIMITED_EDITOR_" + System.nanoTime();
        String username = "perm" + System.nanoTime();
        long eastSalesId = departmentId("East Sales");
        Permission performanceAccess = permissionRepository.findByCode(PermissionCodes.PERFORMANCE_ACCESS).orElseThrow();
        Permission viewSelf = permissionRepository.findByCode(PermissionCodes.PERFORMANCE_VIEW_SELF).orElseThrow();
        Permission createPerformance = permissionRepository.findByCode(PermissionCodes.PERFORMANCE_CREATE).orElseThrow();

        MvcResult roleCreationResult = mockMvc.perform(post("/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "code": "%s",
                      "name": "受限录入角色",
                      "description": "用于验证权限变更是否立即生效"
                    }
                    """.formatted(roleCode)))
            .andExpect(status().isOk())
            .andReturn();

        long roleId = objectMapper.readTree(roleCreationResult.getResponse().getContentAsString())
            .path("data")
            .path("id")
            .asLong();

        mockMvc.perform(post("/roles/" + roleId + "/permissions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "permissionIds": [%d, %d, %d]
                    }
                    """.formatted(performanceAccess.getId(), viewSelf.getId(), createPerformance.getId())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "Passw0rd!",
                      "displayName": "权限验证用户",
                      "departmentId": %d,
                      "email": "%s@atlas.local",
                      "phone": "13800000123"
                    }
                    """.formatted(username, eastSalesId, username)))
            .andExpect(status().isOk());

        UserAccount permissionUser = userRepository.findByUsername(username).orElseThrow();
        mockMvc.perform(post("/users/" + permissionUser.getId() + "/roles")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "roleIds": [%d]
                    }
                    """.formatted(roleId)))
            .andExpect(status().isOk());

        String permissionUserToken = login(username, "Passw0rd!");
        mockMvc.perform(post("/performance/records")
                .header("Authorization", "Bearer " + permissionUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": 52000,
                      "occurredOn": "%s",
                      "type": "新客签约",
                      "note": "权限变更前允许录入"
                    }
                    """.formatted(LocalDate.now())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/roles/" + roleId + "/permissions")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "permissionIds": [%d, %d]
                    }
                    """.formatted(performanceAccess.getId(), viewSelf.getId())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/performance/records")
                .header("Authorization", "Bearer " + permissionUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "amount": 53000,
                      "occurredOn": "%s",
                      "type": "续费回款",
                      "note": "权限变更后应被拒绝"
                    }
                    """.formatted(LocalDate.now())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void portalAppsShouldBeFilteredByCurrentUserPermissions() throws Exception {
        String adminToken = login("admin", "Admin@123");
        mockMvc.perform(get("/portal/apps")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.key=='permission')]").exists())
            .andExpect(jsonPath("$.data[?(@.key=='performance')]").exists())
            .andExpect(jsonPath("$.data[?(@.key=='approval')]").doesNotExist())
            .andExpect(jsonPath("$.data[?(@.key=='docs')]").doesNotExist());

        String employeeToken = login("employee", "Employee@123");
        mockMvc.perform(get("/portal/apps")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.key=='oa')]").exists())
            .andExpect(jsonPath("$.data[?(@.key=='performance')]").exists())
            .andExpect(jsonPath("$.data[?(@.key=='permission')]").doesNotExist())
            .andExpect(jsonPath("$.data[?(@.key=='approval')]").doesNotExist());

        String managerToken = login("manager", "Manager@123");
        mockMvc.perform(get("/portal/apps")
                .header("Authorization", "Bearer " + managerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.key=='performance')]").exists())
            .andExpect(jsonPath("$.data[?(@.key=='approval')]").doesNotExist());
    }

    @Test
    void portalAuthorizationShouldIssueTicketAndAuditAccess() throws Exception {
        String employeeToken = login("employee", "Employee@123");

        MvcResult authorizationResult = mockMvc.perform(post("/portal/apps/finance/authorize")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appKey").value("finance"))
            .andExpect(jsonPath("$.data.accessTicket").isNotEmpty())
            .andExpect(jsonPath("$.data.targetPath").value("/systems/finance"))
            .andExpect(jsonPath("$.data.entryPath").isNotEmpty())
            .andReturn();

        String ticket = objectMapper.readTree(authorizationResult.getResponse().getContentAsString())
            .path("data")
            .path("accessTicket")
            .asText();

        mockMvc.perform(post("/portal/sso/tickets/" + ticket + "/verify")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appKey").value("finance"))
            .andExpect(jsonPath("$.data.status").value("VERIFIED"))
            .andExpect(jsonPath("$.data.user.username").value("employee"));

        mockMvc.perform(get("/portal/apps")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isOk());

        String adminToken = login("admin", "Admin@123");
        mockMvc.perform(get("/logs?limit=20")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.action=='APP_ACCESS' && @.resourceId=='finance' && @.result=='SUCCESS')]").exists());
    }

    @Test
    void portalAuthorizationShouldRejectUnauthorizedApp() throws Exception {
        String employeeToken = login("employee", "Employee@123");

        mockMvc.perform(post("/portal/apps/permission/authorize")
                .header("Authorization", "Bearer " + employeeToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("APP_ACCESS_DENIED"));
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s"
                    }
                    """.formatted(username, password)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    private long departmentId(String name) {
        return departmentRepository.findByName(name).orElseThrow().getId();
    }
}
