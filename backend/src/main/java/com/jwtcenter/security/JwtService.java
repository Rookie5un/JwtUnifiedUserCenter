package com.jwtcenter.security;

import com.jwtcenter.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenMinutes;

    public JwtService(
        @Value("${app.security.jwt-secret}") String secret,
        @Value("${app.security.access-token-minutes}") long accessTokenMinutes
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
            .subject(user.getUsername())
            .claims(Map.of(
                "uid", user.getId(),
                "displayName", user.getDisplayName(),
                "department", user.getDepartment(),
                "type", "ACCESS"
            ))
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }

    public Instant getAccessTokenExpiry() {
        return Instant.now().plus(accessTokenMinutes, ChronoUnit.MINUTES);
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean isExpired(String token) {
        try {
            return parse(token).getPayload().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return true;
        }
    }
}
