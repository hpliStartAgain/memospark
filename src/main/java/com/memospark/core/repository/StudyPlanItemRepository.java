package com.memospark.core.repository;

import com.memospark.core.domain.StudyPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface StudyPlanItemRepository extends JpaRepository<StudyPlanItem, Long> {
    List<StudyPlanItem> findByPlanIdOrderByPlanDateAscSortOrderAsc(Long planId);
    @Query("SELECT item FROM StudyPlanItem item " +
           "WHERE item.plan.user.id = :userId AND item.plan.active = true AND item.planDate = :planDate " +
           "ORDER BY item.sortOrder")
    List<StudyPlanItem> findActiveByUserAndDate(
            @Param("userId") Long userId, @Param("planDate") LocalDate planDate);
}
