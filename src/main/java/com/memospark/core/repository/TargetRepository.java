package com.memospark.core.repository;

import com.memospark.core.domain.Target;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
