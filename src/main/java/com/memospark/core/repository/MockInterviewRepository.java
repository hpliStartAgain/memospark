package com.memospark.core.repository;

import com.memospark.core.domain.MockInterview;
import com.memospark.core.domain.MockInterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MockInterviewRepository extends JpaRepository<MockInterview, Long> {

    List<MockInterview> findByTargetIdOrderByStartedAtDesc(Long targetId);

    Optional<MockInterview> findByIdAndTargetId(Long id, Long targetId);

    @Query("SELECT COALESCE(AVG(mi.averageScore), 0) FROM MockInterview mi " +
           "WHERE mi.target.id = :targetId AND mi.status = :status " +
           "AND mi.averageScore IS NOT NULL AND mi.finishedAt >= :since")
    double calculateAverageScoreByTargetIdSince(@Param("targetId") Long targetId,
                                                @Param("status") MockInterviewStatus status,
                                                @Param("since") LocalDateTime since);

    long countByTargetIdAndStatusAndFinishedAtGreaterThanEqual(Long targetId,
                                                               MockInterviewStatus status,
                                                               LocalDateTime since);
}
