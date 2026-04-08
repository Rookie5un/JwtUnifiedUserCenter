package com.jwtcenter.service;

import com.jwtcenter.dto.token.TokenInspectResponse;
import com.jwtcenter.dto.user.UserResponse;
import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.repository.UserRepository;
import com.jwtcenter.security.JwtService;
import com.jwtcenter.util.MapperUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TokenInspectionService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public TokenInspectionService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public TokenInspectResponse inspect(String token) {
        try {
            Jws<Claims> parsed = jwtService.parse(token);
            return buildResponse(parsed.getPayload(), false);
        } catch (ExpiredJwtException exception) {
            return buildResponse(exception.getClaims(), true);
        } catch (JwtException | IllegalArgumentException exception) {
            return new TokenInspectResponse(false, true, null, null, List.of(), List.of());
        }
    }

    private TokenInspectResponse buildResponse(Claims claims, boolean expired) {
        Long userId = ((Number) claims.get("uid")).longValue();
        UserAccount user = userRepository.findById(userId).orElse(null);
        UserResponse userResponse = user == null ? null : MapperUtils.toUserResponse(user);
        List<String> roles = userResponse == null ? List.of() : userResponse.roles();
        List<String> permissions = userResponse == null ? List.of() : userResponse.permissions();
        Instant expiresAt = claims.getExpiration() == null ? null : claims.getExpiration().toInstant();
        return new TokenInspectResponse(!expired, expired, expiresAt, userResponse, roles, permissions);
    }
}
