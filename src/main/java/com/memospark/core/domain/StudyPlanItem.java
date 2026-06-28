package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "study_plan_items")
@Getter
@Setter
@NoArgsConstructor
public class StudyPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id")
    private Deck deck;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    private int weekNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StudyPlanItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearningStage learningStage;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String objective;

    @Column(nullable = false)
    private int targetCount = 1;

    @Column(nullable = false)
    private int sortOrder = 0;

    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

