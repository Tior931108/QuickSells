package com.example.quicksells.domain.user.repository;

import com.example.quicksells.common.enums.UserRole;
import com.example.quicksells.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    Page<User> findAllByRole(UserRole role, Pageable pageable);

    // 특정 권한 목록으로 사용자 조회
    List<User> findByRoleIn(List<UserRole> roles);

    Optional<User> findByProviderId(String providerId);
}
