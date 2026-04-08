package com.jwtcenter.service;

import com.jwtcenter.dto.permission.PermissionRequest;
import com.jwtcenter.dto.permission.PermissionResponse;
import com.jwtcenter.entity.Permission;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.PermissionRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public PermissionService(
        PermissionRepository permissionRepository,
        AccessService accessService,
        OperationLogService operationLogService
    ) {
        this.permissionRepository = permissionRepository;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        accessService.requireAnyPermission(
            accessService.currentUser(),
            PermissionCodes.ROLE_MANAGE,
            PermissionCodes.PERMISSION_MANAGE
        );
        return permissionRepository.findAll().stream().map(MapperUtils::toPermissionResponse).toList();
    }

    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERMISSION_MANAGE);
        permissionRepository.findByCode(request.code()).ifPresent(permission -> {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PERMISSION_EXISTS", "Permission code already exists.");
        });
        Permission permission = new Permission();
        apply(permission, request);
        Permission saved = permissionRepository.save(permission);
        operationLogService.log(actor, "CREATE_PERMISSION", "PERMISSION", String.valueOf(saved.getId()), OperationResult.SUCCESS, "Permission created.");
        return MapperUtils.toPermissionResponse(saved);
    }

    @Transactional
    public PermissionResponse updatePermission(Long permissionId, PermissionRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERMISSION_MANAGE);
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PERMISSION_NOT_FOUND", "Permission not found."));
        apply(permission, request);
        Permission saved = permissionRepository.save(permission);
        operationLogService.log(actor, "UPDATE_PERMISSION", "PERMISSION", String.valueOf(permissionId), OperationResult.SUCCESS, "Permission updated.");
        return MapperUtils.toPermissionResponse(saved);
    }

    @Transactional
    public void deletePermission(Long permissionId) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.PERMISSION_MANAGE);
        Permission permission = permissionRepository.findById(permissionId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PERMISSION_NOT_FOUND", "Permission not found."));
        permissionRepository.delete(permission);
        operationLogService.log(actor, "DELETE_PERMISSION", "PERMISSION", String.valueOf(permissionId), OperationResult.SUCCESS, "Permission deleted.");
    }

    private void apply(Permission permission, PermissionRequest request) {
        permission.setCode(request.code());
        permission.setName(request.name());
        permission.setResource(request.resource());
        permission.setAction(request.action());
        permission.setType(request.type());
        permission.setDescription(request.description());
    }
}
