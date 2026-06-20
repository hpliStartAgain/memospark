package com.memospark.core.repository;

import com.memospark.core.domain.TargetSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TargetSkillRepository extends JpaRepository<TargetSkill, Long> {
    List<TargetSkill> findByTargetIdOrderByWeightDescIdAsc(Long targetId);
    long countByTargetId(Long targetId);

    @Transactional
    void deleteByTargetId(Long targetId);
}
