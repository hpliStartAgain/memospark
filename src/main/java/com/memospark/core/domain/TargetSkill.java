package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "target_skills")
@Getter
@Setter
@NoArgsConstructor
public class TargetSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private Target target;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String description;

    /** Importance derived from JD frequency / recruiter analysis. Range 1-5. */
    @Column(nullable = false)
    private int weight = 3;

    /** Self-assessed mastery. Range 0-5 (0 = not started). */
    @Column(nullable = false)
    private int selfLevel = 0;

    /** Study deck generated for this skill (JD → skill → deck → cards loop). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    /** AI-proposed sub-topics (one per line) used to generate cards on demand. */
    @Column(length = 2000)
    private String topics;

    /** AI-suggested total card count for this skill's deck. */
    @Column(name = "suggested_card_count", nullable = false)
    private int suggestedCardCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public TargetSkill(Target target, User user, String name, String category, String description, int weight) {
        this.target = target;
        this.user = user;
        this.name = name;
        this.category = category;
        this.description = description;
        this.weight = weight;
    }
}
