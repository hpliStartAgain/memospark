package com.memospark.core.repository;

import com.memospark.core.domain.CodeProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CodeProblemRepository extends JpaRepository<CodeProblem, Long> {

    List<CodeProblem> findAllByOrderByProblemNumberAsc();

    Optional<CodeProblem> findByProblemNumber(Integer problemNumber);

    /**
     * Lightweight projection used by startup initializer to check existence
     * without loading full rows. Avoids N round-trips of findByProblemNumber.
     */
    @Query("select c.problemNumber from CodeProblem c")
    List<Integer> findAllProblemNumbers();

    /**
     * Used by startup initializer to backfill missing categories in one query.
     */
    @Query("select c from CodeProblem c where c.category is null or c.category = ''")
    List<CodeProblem> findAllWithMissingCategory();
}
