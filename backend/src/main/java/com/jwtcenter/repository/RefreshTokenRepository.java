package com.jwtcenter.repository;

import com.jwtcenter.entity.RefreshToken;
import com.jwtcenter.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUser(UserAccount user);
}
