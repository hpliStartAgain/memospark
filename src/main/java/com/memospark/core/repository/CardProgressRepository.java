package com.memospark.core.repository;

import com.memospark.core.domain.CardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardProgressRepository extends JpaRepository<CardProgress, Long> {

    Optional<CardProgress> findByCardId(Long cardId);

    @Query("SELECT cp FROM CardProgress cp WHERE cp.card.id IN :cardIds")
    List<CardProgress> findByCardIdIn(@Param("cardIds") List<Long> cardIds);

    List<CardProgress> findByNextReviewDateLessThanEqual(LocalDate date);

    @Query("SELECT cp FROM CardProgress cp JOIN cp.card c WHERE c.deck.user.id = :userId AND cp.nextReviewDate <= :date")
    List<CardProgress> findDueByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT cp FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.nextReviewDate <= :date")
    List<CardProgress> findDueByDeckId(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp WHERE cp.nextReviewDate <= :date")
    long countDueCards(@Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.user.id = :userId AND cp.nextReviewDate <= :date")
    long countDueCardsByUserId(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.nextReviewDate <= :date")
    long countDueByDeckId(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.nextReviewDate <= :date AND cp.lastReviewDate IS NULL")
    long countNewDueByDeckId(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.nextReviewDate <= :date AND cp.lastReviewDate IS NOT NULL")
    long countReviewDueByDeckId(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.firstLearnedDate = :date")
    long countFirstLearnedTodayByDeckId(@Param("deckId") Long deckId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(cp) FROM CardProgress cp JOIN cp.card c WHERE c.deck.id = :deckId AND cp.firstLearnedDate = :date AND c.learningStage = :stage")
    long countFirstLearnedByDeckIdAndDateAndStage(
            @Param("deckId") Long deckId,
            @Param("date") LocalDate date,
            @Param("stage") com.memospark.core.domain.LearningStage stage);

    void deleteByCardId(Long cardId);
}
