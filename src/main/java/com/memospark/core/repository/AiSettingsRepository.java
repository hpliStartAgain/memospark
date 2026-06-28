package com.memospark.core.repository;

import com.memospark.core.domain.AiSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiSettingsRepository extends JpaRepository<AiSettings, Long> {
    Optional<AiSettings> findByUserId(Long userId);
}
