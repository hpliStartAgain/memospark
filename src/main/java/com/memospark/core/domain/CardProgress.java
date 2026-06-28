package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "card_progress")
@Getter
@Setter
@NoArgsConstructor
public class CardProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private Card card;

    @Column(nullable = false)
    private int repetitions = 0;

    @Column(nullable = false)
    private double easeFactor = 2.5;

    @Column(nullable = false)
    private double stability = 0.0;

    @Column(nullable = false)
    private double difficulty = 5.0;

    @Column(name = "review_interval", nullable = false)
    private int interval = 0;

    @Column(nullable = false)
    private LocalDate nextReviewDate = LocalDate.now();

    private LocalDate lastReviewDate;

    /** The date this card was first ever reviewed (learned). */
    private LocalDate firstLearnedDate;

    public CardProgress(Card card) {
        this.card = card;
    }
}
