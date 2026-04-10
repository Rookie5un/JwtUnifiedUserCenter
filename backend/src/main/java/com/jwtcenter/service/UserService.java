package com.jwtcenter.service;

import com.jwtcenter.dto.user.AssignRolesRequest;
import com.jwtcenter.dto.user.ResetPasswordRequest;
import com.jwtcenter.dto.user.UpdateUserRequest;
import com.jwtcenter.dto.user.UpdateUserStatusRequest;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.enums.UserStatus;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.RefreshTokenRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.PermissionCodes;
import com.jwtcenter.util.MapperUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;
    private final DepartmentService departmentService;
    private final OperationLogService operationLogService;

    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        AccessService accessService,
        DepartmentService departmentService,
        OperationLogService operationLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessService = accessService;
        this.departmentService = departmentService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        accessService.requirePermission(accessService.currentUser(), PermissionCodes.USER_MANAGE);
        return userRepository.findAllByDeletedAtIsNullOrderByCreatedAtDesc().stream().map(MapperUtils::toUserResponse).toList();
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
        if (canManageUsers && !user.getUsername().equals(request.username()) && userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USERNAME_EXISTS", "Username already exists.");
        }
        if (canManageUsers) {
            departmentService.requireExistingDepartment(request.department());
            user.setUsername(request.username());
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

    @Transactional
    public void deleteUser(Long userId) {
        UserAccount actor = accessService.currentUser();
        accessService.requirePermission(actor, PermissionCodes.USER_MANAGE);
        if (actor.getId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SELF_DELETE_FORBIDDEN", "You cannot delete your current account.");
        }

        UserAccount user = findUser(userId);
        boolean deletingActiveAdmin = accessService.hasRole(user, "ADMIN") && user.getStatus() == UserStatus.ACTIVE;
        if (deletingActiveAdmin && userRepository.countDistinctByRoles_CodeAndStatusAndDeletedAtIsNull("ADMIN", UserStatus.ACTIVE) <= 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "LAST_ADMIN_FORBIDDEN", "At least one active administrator account must remain.");
        }

        Instant deletedAt = Instant.now();
        user.setDeletedAt(deletedAt);
        refreshTokenRepository.findByUser(user).forEach(token -> token.setRevokedAt(deletedAt));
        userRepository.save(user);
        operationLogService.log(actor, "DELETE_USER", "USER", String.valueOf(userId), OperationResult.SUCCESS, "User logically deleted.");
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }
}
