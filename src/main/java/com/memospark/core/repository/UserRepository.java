package com.memospark.core.repository;

import com.memospark.core.domain.User;
import com.memospark.core.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByApiKey(String apiKey);
    Optional<User> findByWxOpenid(String wxOpenid);

    long countByRole(UserRole role);

    long countByEnabledTrue();

    long countByCreatedAtAfter(LocalDateTime since);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<User> findAllByOrderByCreatedAtDesc();
}
