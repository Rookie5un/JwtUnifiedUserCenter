package com.jwtcenter.service;

import com.jwtcenter.dto.role.AssignPermissionsRequest;
import com.jwtcenter.dto.role.RoleRequest;
import com.jwtcenter.dto.role.RoleResponse;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public RoleService(
        RoleRepository roleRepository,
        PermissionRepository permissionRepository,
        AccessService accessService,
        OperationLogService operationLogService
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        accessService.requireAnyPermission(
            accessService.currentUser(),
            PermissionCodes.USER_MANAGE,
            PermissionCodes.ROLE_MANAGE
        );
        return roleRepository.findAll().stream().map(MapperUtils::toRoleResponse).toList();
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.ROLE_MANAGE);
        roleRepository.findByCode(request.code()).ifPresent(role -> {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_EXISTS", "Role code already exists.");
        });
        Role role = new Role();
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        operationLogService.log(actor, "CREATE_ROLE", "ROLE", String.valueOf(saved.getId()), OperationResult.SUCCESS, "Role created.");
        return MapperUtils.toRoleResponse(saved);
    }

    @Transactional
    public RoleResponse updateRole(Long roleId, RoleRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.ROLE_MANAGE);
        Role role = findRole(roleId);
        role.setCode(request.code());
        role.setName(request.name());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        operationLogService.log(actor, "UPDATE_ROLE", "ROLE", String.valueOf(roleId), OperationResult.SUCCESS, "Role updated.");
        return MapperUtils.toRoleResponse(saved);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.ROLE_MANAGE);
        roleRepository.delete(findRole(roleId));
        operationLogService.log(actor, "DELETE_ROLE", "ROLE", String.valueOf(roleId), OperationResult.SUCCESS, "Role deleted.");
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, AssignPermissionsRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.ROLE_MANAGE);
        Role role = findRole(roleId);
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.permissionIds()));
        if (permissions.size() != request.permissionIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERMISSION_NOT_FOUND", "One or more permissions do not exist.");
        }
        role.setPermissions(permissions);
        Role saved = roleRepository.save(role);
        operationLogService.log(actor, "ASSIGN_PERMISSIONS", "ROLE", String.valueOf(roleId), OperationResult.SUCCESS, "Role permissions updated.");
        return MapperUtils.toRoleResponse(saved);
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "Role not found."));
    }
}
