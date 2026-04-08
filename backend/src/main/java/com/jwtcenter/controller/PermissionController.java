package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.permission.PermissionRequest;
import com.jwtcenter.dto.permission.PermissionResponse;
import com.jwtcenter.service.PermissionService;
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
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<List<PermissionResponse>> listPermissions() {
        return ApiResponse.success("Permissions loaded.", permissionService.listPermissions());
    }

    @PostMapping
    public ApiResponse<PermissionResponse> createPermission(@Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success("Permission created.", permissionService.createPermission(request));
    }

    @PutMapping("/{permissionId}")
    public ApiResponse<PermissionResponse> updatePermission(@PathVariable Long permissionId, @Valid @RequestBody PermissionRequest request) {
        return ApiResponse.success("Permission updated.", permissionService.updatePermission(permissionId, request));
    }

    @DeleteMapping("/{permissionId}")
    public ApiResponse<Void> deletePermission(@PathVariable Long permissionId) {
        permissionService.deletePermission(permissionId);
        return ApiResponse.success("Permission deleted.", null);
    }
}
