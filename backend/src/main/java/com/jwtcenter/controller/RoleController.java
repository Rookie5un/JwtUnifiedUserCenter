package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.role.AssignPermissionsRequest;
import com.jwtcenter.dto.role.RoleRequest;
import com.jwtcenter.dto.role.RoleResponse;
import com.jwtcenter.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> listRoles() {
        return ApiResponse.success("Roles loaded.", roleService.listRoles());
    }

    @PostMapping
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return ApiResponse.success("Role created.", roleService.createRole(request));
    }

    @PutMapping("/{roleId}")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long roleId, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.success("Role updated.", roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    public ApiResponse<Void> deleteRole(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return ApiResponse.success("Role deleted.", null);
    }

    @PostMapping("/{roleId}/permissions")
    public ApiResponse<RoleResponse> assignPermissions(@PathVariable Long roleId, @Valid @RequestBody AssignPermissionsRequest request) {
        return ApiResponse.success("Role permissions updated.", roleService.assignPermissions(roleId, request));
    }
}
