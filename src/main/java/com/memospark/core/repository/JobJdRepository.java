package com.memospark.core.repository;

import com.memospark.core.domain.JobJd;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobJdRepository extends JpaRepository<JobJd, Long> {
    List<JobJd> findByTargetIdOrderByCreatedAtDesc(Long targetId);
    long countByTargetId(Long targetId);
}
