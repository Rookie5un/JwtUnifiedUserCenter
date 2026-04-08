package com.jwtcenter.service;

import com.jwtcenter.dto.auth.AuthTokenResponse;
import com.jwtcenter.dto.auth.ChangePasswordRequest;
import com.jwtcenter.dto.auth.LoginRequest;
import com.jwtcenter.dto.auth.RefreshTokenRequest;
import com.jwtcenter.dto.auth.RegisterRequest;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.entity.RefreshToken;
import com.jwtcenter.entity.Role;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.OperationResult;
import com.jwtcenter.enums.UserStatus;
import com.jwtcenter.exception.ApiException;
import com.jwtcenter.repository.RefreshTokenRepository;
import com.jwtcenter.repository.RoleRepository;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.JwtService;
import com.jwtcenter.util.MapperUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccessService accessService;
    private final OperationLogService operationLogService;
    private final long refreshTokenDays;

    public AuthService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AccessService accessService,
        OperationLogService operationLogService,
        @Value("${app.security.refresh-token-days}") long refreshTokenDays
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.accessService = accessService;
        this.operationLogService = operationLogService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USERNAME_EXISTS", "Username already exists.");
        }
        Role employeeRole = roleRepository.findByCode("EMPLOYEE")
            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ROLE_MISSING", "Default employee role is missing."));
        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setDepartment(request.department());
        user.setEmail(request.email());
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);
        user.getRoles().add(employeeRole);
        UserAccount saved = userRepository.save(user);
        operationLogService.log(saved, "REGISTER", "USER", String.valueOf(saved.getId()), OperationResult.SUCCESS, "Self registration completed.");
        return MapperUtils.toUserResponse(saved);
    }

    @Transactional
    public AuthTokenResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> {
                operationLogService.log(null, "LOGIN", "USER", request.username(), OperationResult.FAILURE, "Unknown username.");
                return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
            });
        if (user.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            operationLogService.log(user, "LOGIN", "USER", String.valueOf(user.getId()), OperationResult.FAILURE, "Invalid password or disabled account.");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
        }
        AuthTokenResponse response = issueTokens(user);
        operationLogService.log(user, "LOGIN", "USER", String.valueOf(user.getId()), OperationResult.SUCCESS, "Login succeeded.");
        return response;
    }

    @Transactional
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid."));
        if (existing.getRevokedAt() != null || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is expired or revoked.");
        }
        existing.setRevokedAt(Instant.now());
        UserAccount user = existing.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_DISABLED", "User is disabled.");
        }
        AuthTokenResponse response = issueTokens(user);
        operationLogService.log(user, "TOKEN_REFRESH", "REFRESH_TOKEN", String.valueOf(existing.getId()), OperationResult.SUCCESS, "Issued new access token.");
        return response;
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid."));
        existing.setRevokedAt(Instant.now());
        operationLogService.log(existing.getUser(), "LOGOUT", "REFRESH_TOKEN", String.valueOf(existing.getId()), OperationResult.SUCCESS, "Refresh token revoked.");
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        return MapperUtils.toUserResponse(accessService.currentUser());
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserAccount user = accessService.currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "Current password is incorrect.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.findByUser(user).forEach(token -> token.setRevokedAt(Instant.now()));
        operationLogService.log(user, "CHANGE_PASSWORD", "USER", String.valueOf(user.getId()), OperationResult.SUCCESS, "Password changed.");
    }

    private AuthTokenResponse issueTokens(UserAccount user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);
        return new AuthTokenResponse(
            accessToken,
            refreshToken.getToken(),
            jwtService.getAccessTokenExpiry(),
            MapperUtils.toUserResponse(user)
        );
    }
}
