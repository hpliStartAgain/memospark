package com.memospark.core.repository;

import com.memospark.core.domain.Deck;
import com.memospark.core.domain.DeckType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByType(DeckType type);
    Optional<Deck> findByNameAndType(String name, DeckType type);
    List<Deck> findByUserId(Long userId);
    long countByUserId(Long userId);
    Optional<Deck> findByUserIdAndNameAndType(Long userId, String name, DeckType type);
}
