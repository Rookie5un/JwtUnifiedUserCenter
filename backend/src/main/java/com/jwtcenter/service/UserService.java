package com.jwtcenter.service;

import com.jwtcenter.dto.user.AssignRolesRequest;
import com.jwtcenter.dto.user.ResetPasswordRequest;
import com.jwtcenter.dto.user.UpdateUserRequest;
import com.jwtcenter.dto.user.UpdateUserStatusRequest;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;
    private final OperationLogService operationLogService;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AccessService accessService,
        OperationLogService operationLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        accessService.requirePermission(accessService.currentUser(), PermissionCodes.USER_MANAGE);
        return userRepository.findAll().stream().map(MapperUtils::toUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        UserAccount actor = accessService.currentUser();
        if (!actor.getId().equals(userId) && !accessService.hasPermission(actor, PermissionCodes.USER_MANAGE)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only view your own profile.");
        }
        return MapperUtils.toUserResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {
        UserAccount actor = accessService.currentUser();
        UserAccount user = findUser(userId);
        boolean canManageUsers = accessService.hasPermission(actor, PermissionCodes.USER_MANAGE);
        if (!canManageUsers && !actor.getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "You can only update your own profile.");
        }
        user.setDisplayName(request.displayName());
        user.setPhone(request.phone());
        user.setEmail(request.email());
        if (canManageUsers) {
            user.setDepartment(request.department());
        }
        UserAccount saved = userRepository.save(user);
        operationLogService.log(actor, "UPDATE_USER", "USER", String.valueOf(userId), OperationResult.SUCCESS, "Updated user profile.");
        return MapperUtils.toUserResponse(saved);
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UpdateUserStatusRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        UserAccount user = findUser(userId);
        user.setStatus(request.status());
        UserAccount saved = userRepository.save(user);
        operationLogService.log(actor, "UPDATE_USER_STATUS", "USER", String.valueOf(userId), OperationResult.SUCCESS, "Updated account status to " + request.status());
        return MapperUtils.toUserResponse(saved);
    }

    @Transactional
    public void resetPassword(Long userId, ResetPasswordRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        UserAccount user = findUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        operationLogService.log(actor, "RESET_PASSWORD", "USER", String.valueOf(userId), OperationResult.SUCCESS, "Password reset by administrator.");
    }

    @Transactional
    public UserResponse assignRoles(Long userId, AssignRolesRequest request) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        UserAccount user = findUser(userId);
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
        if (roles.size() != request.roleIds().size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND", "One or more roles do not exist.");
        }
        user.setRoles(roles);
        UserAccount saved = userRepository.save(user);
        operationLogService.log(actor, "ASSIGN_ROLES", "USER", String.valueOf(userId), OperationResult.SUCCESS, "Updated user roles.");
        return MapperUtils.toUserResponse(saved);
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }
}
