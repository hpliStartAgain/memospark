package com.memospark.core.repository;

import com.memospark.core.domain.StudyPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyPlanRepository extends JpaRepository<StudyPlan, Long> {
    Optional<StudyPlan> findFirstByTargetIdAndUserIdAndActiveTrueOrderByCreatedAtDesc(Long targetId, Long userId);
    List<StudyPlan> findByTargetIdAndUserIdAndActiveTrue(Long targetId, Long userId);
    List<StudyPlan> findByUserIdAndActiveTrueOrderByCreatedAtDesc(Long userId);
}

