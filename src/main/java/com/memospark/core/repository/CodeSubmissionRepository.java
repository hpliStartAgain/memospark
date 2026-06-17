package com.memospark.core.repository;

import com.memospark.core.domain.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {

    List<CodeSubmission> findByProblemIdOrderBySubmittedAtDesc(Long problemId);

    boolean existsByProblemIdAndStatus(Long problemId, String status);

    List<CodeSubmission> findByProblemIdAndUserIdOrderBySubmittedAtDesc(Long problemId, Long userId);

    boolean existsByProblemIdAndUserIdAndStatus(Long problemId, Long userId, String status);

    void deleteByProblemId(Long problemId);

    @Query("SELECT s.problem.id, COUNT(s) FROM CodeSubmission s WHERE s.user.id = :userId AND s.status <> 'ACCEPTED' GROUP BY s.problem.id")
    List<Object[]> countFailsByUser(Long userId);

    @Query("SELECT s.problem.id, COUNT(s) FROM CodeSubmission s WHERE s.user.id = :userId GROUP BY s.problem.id")
    List<Object[]> countAttemptsByUser(Long userId);

    @Query("SELECT DISTINCT s.problem.id FROM CodeSubmission s WHERE s.user.id = :userId AND s.status = 'ACCEPTED'")
    List<Long> findAcceptedProblemIdsByUser(Long userId);

    int countByProblemIdAndUserId(Long problemId, Long userId);
}
