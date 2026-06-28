package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "review_logs")
@Getter
@Setter
@NoArgsConstructor
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private LocalDate reviewDate = LocalDate.now();

    @Column(nullable = false)
    private int quality; // 0-5

    private Long timeSpentMs;

    // Snapshot of CardProgress BEFORE this review (for undo)
    private int prevRepetitions;
    private double prevEaseFactor;
    private double prevStability;
    private double prevDifficulty;
    private int prevInterval;
    private LocalDate prevNextReviewDate;
    private LocalDate prevLastReviewDate;
    private LocalDate prevFirstLearnedDate;

    public ReviewLog(Card card, int quality, Long timeSpentMs) {
        this.card = card;
        this.quality = quality;
        this.timeSpentMs = timeSpentMs;
    }
}
