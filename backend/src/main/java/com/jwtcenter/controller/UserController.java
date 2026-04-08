package com.jwtcenter.controller;

import com.jwtcenter.dto.common.ApiResponse;
import com.jwtcenter.dto.user.AssignRolesRequest;
import com.jwtcenter.dto.user.ResetPasswordRequest;
import com.jwtcenter.dto.user.UpdateUserRequest;
import com.jwtcenter.dto.user.UpdateUserStatusRequest;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> listUsers() {
        return ApiResponse.success("Users loaded.", userService.listUsers());
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable Long userId) {
        return ApiResponse.success("User loaded.", userService.getUser(userId));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success("User updated.", userService.updateUser(userId, request));
    }

    @PutMapping("/{userId}/status")
    public ApiResponse<UserResponse> updateUserStatus(@PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success("User status updated.", userService.updateStatus(userId, request));
    }

    @PostMapping("/{userId}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(userId, request);
        return ApiResponse.success("Password reset.", null);
    }

    @PostMapping("/{userId}/roles")
    public ApiResponse<UserResponse> assignRoles(@PathVariable Long userId, @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.success("Roles assigned.", userService.assignRoles(userId, request));
    }
}
