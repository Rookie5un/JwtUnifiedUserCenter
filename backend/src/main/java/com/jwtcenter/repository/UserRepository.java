package com.jwtcenter.repository;

import com.jwtcenter.entity.UserAccount;
import com.jwtcenter.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByIdAndDeletedAtIsNull(Long id);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    List<UserAccount> findAllByDeletedAtIsNullOrderByCreatedAtDesc();

    List<UserAccount> findAllByDepartment(String department);

    long countByDepartmentAndDeletedAtIsNull(String department);

    long countDistinctByRoles_CodeAndStatusAndDeletedAtIsNull(String roleCode, UserStatus status);
}
