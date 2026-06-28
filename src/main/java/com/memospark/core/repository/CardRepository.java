package com.memospark.core.repository;

import com.memospark.core.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByDeckIdOrderByLearningStageAscStageOrderAscIdAsc(Long deckId);
    default List<Card> findByDeckId(Long deckId) {
        return findByDeckIdOrderByLearningStageAscStageOrderAscIdAsc(deckId);
    }
    long countByDeckId(Long deckId);
    long countByDeckIdAndLearningStage(Long deckId, com.memospark.core.domain.LearningStage learningStage);
    void deleteByDeckId(Long deckId);

    @Query("SELECT COUNT(c) FROM Card c WHERE c.deck.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT cp FROM CardProgress cp WHERE cp.card.id IN :cardIds")
    java.util.List<com.memospark.core.domain.CardProgress> findProgressByCardIdIn(@Param("cardIds") java.util.List<Long> cardIds);

    @Query("SELECT DISTINCT c.tags FROM Card c WHERE c.deck.id = :deckId AND c.tags IS NOT NULL AND c.tags <> ''")
    List<String> findDistinctTagsByDeckId(@Param("deckId") Long deckId);
}
