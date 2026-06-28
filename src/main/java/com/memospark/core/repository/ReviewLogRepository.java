package com.memospark.core.repository;

import com.memospark.core.domain.ReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    long countByReviewDateBetween(LocalDate start, LocalDate end);

    List<ReviewLog> findByReviewDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl WHERE rl.reviewDate = :date")
    long countByReviewDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId AND rl.reviewDate = :date")
    long countByUserIdAndReviewDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.id = :deckId AND rl.reviewDate = :date")
    long countByDeckIdAndReviewDate(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId AND rl.reviewDate = :date AND rl.learningMode = :learningMode")
    long countByUserIdAndReviewDateAndLearningMode(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("learningMode") String learningMode);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.id = :deckId AND rl.reviewDate = :date AND rl.learningMode = :learningMode")
    long countByDeckIdAndReviewDateAndLearningMode(
            @Param("deckId") Long deckId,
            @Param("date") LocalDate date,
            @Param("learningMode") String learningMode);

    @Query("SELECT COALESCE(AVG(CASE WHEN rl.quality >= 3 THEN 1.0 ELSE 0.0 END), 0) FROM ReviewLog rl")
    double calculateOverallRetentionRate();

    @Query("SELECT COALESCE(AVG(CASE WHEN rl.quality >= 3 THEN 1.0 ELSE 0.0 END), 0) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId")
    double calculateRetentionRateByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT rl.reviewDate FROM ReviewLog rl WHERE rl.reviewDate >= :since ORDER BY rl.reviewDate DESC")
    List<LocalDate> findDistinctReviewDatesSince(@Param("since") LocalDate since);

    @Query("SELECT DISTINCT rl.reviewDate FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId AND rl.reviewDate >= :since ORDER BY rl.reviewDate DESC")
    List<LocalDate> findDistinctReviewDatesSinceByUserId(@Param("userId") Long userId, @Param("since") LocalDate since);

    @Query("SELECT rl FROM ReviewLog rl WHERE rl.card.id = :cardId ORDER BY rl.id DESC LIMIT 1")
    java.util.Optional<ReviewLog> findLatestByCardId(@Param("cardId") Long cardId);

    @Query("SELECT rl.reviewDate, COUNT(rl), COALESCE(AVG(CASE WHEN rl.quality >= 3 THEN 1.0 ELSE 0.0 END), 0) " +
           "FROM ReviewLog rl WHERE rl.reviewDate >= :since GROUP BY rl.reviewDate ORDER BY rl.reviewDate")
    List<Object[]> findDailyStatsSince(@Param("since") LocalDate since);

    @Query("SELECT rl.reviewDate, COUNT(rl), COALESCE(AVG(CASE WHEN rl.quality >= 3 THEN 1.0 ELSE 0.0 END), 0) " +
           "FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId AND rl.reviewDate >= :since GROUP BY rl.reviewDate ORDER BY rl.reviewDate")
    List<Object[]> findDailyStatsSinceByUserId(@Param("userId") Long userId, @Param("since") LocalDate since);

    /** Find card IDs with high fail rate (quality < 3) — at least minFails times, sorted by fail count desc */
    @Query("SELECT rl.card.id, COUNT(rl) FROM ReviewLog rl WHERE rl.quality < 3 GROUP BY rl.card.id HAVING COUNT(rl) >= :minFails ORDER BY COUNT(rl) DESC")
    List<Object[]> findHardCardIds(@Param("minFails") long minFails);

    @Query("SELECT rl.card.id, COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId AND rl.quality < 3 GROUP BY rl.card.id HAVING COUNT(rl) >= :minFails ORDER BY COUNT(rl) DESC")
    List<Object[]> findHardCardIdsByUserId(@Param("userId") Long userId, @Param("minFails") long minFails);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(rl) FROM ReviewLog rl JOIN rl.card c WHERE c.deck.id = :deckId AND rl.reviewDate >= :since")
    long countByDeckIdSince(@Param("deckId") Long deckId, @Param("since") LocalDate since);

    @Query("SELECT COALESCE(AVG(CASE WHEN rl.quality >= 3 THEN 1.0 ELSE 0.0 END), 0) " +
           "FROM ReviewLog rl JOIN rl.card c WHERE c.deck.id = :deckId AND rl.reviewDate >= :since")
    double calculateRetentionRateByDeckIdSince(@Param("deckId") Long deckId, @Param("since") LocalDate since);

    void deleteByCardId(Long cardId);
}
