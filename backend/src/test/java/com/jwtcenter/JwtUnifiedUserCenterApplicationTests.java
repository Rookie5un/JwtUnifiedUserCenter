package com.jwtcenter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
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
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void registerShouldCreateUserWithEncryptedPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "new.employee",
                      "password": "Passw0rd!",
                      "displayName": "新员工",
                      "department": "East Sales",
                      "email": "new.employee@atlas.local",
                      "phone": "13800000099"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.username").value("new.employee"));

        UserAccount saved = userRepository.findByUsername("new.employee").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Passw0rd!");
        assertThat(saved.getPasswordHash()).startsWith("$2");
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

        mockMvc.perform(get("/performance/dashboard/global")
                .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalAmount").value(340000.00))
            .andExpect(jsonPath("$.data.approvedCount").value(2))
            .andExpect(jsonPath("$.data.pendingCount").value(1))
            .andExpect(jsonPath("$.data.rejectedCount").value(1));
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
                      "permissionIds": [%d, %d]
                    }
                    """.formatted(viewSelf.getId(), createPerformance.getId())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "Passw0rd!",
                      "displayName": "权限验证用户",
                      "department": "East Sales",
                      "email": "%s@atlas.local",
                      "phone": "13800000123"
                    }
                    """.formatted(username, username)))
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
                      "permissionIds": [%d]
                    }
                    """.formatted(viewSelf.getId())))
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
}
