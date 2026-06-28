package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mock_interview_questions")
@Getter
@Setter
@NoArgsConstructor
public class MockInterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interview_id", nullable = false)
    private MockInterview interview;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(nullable = false, length = 50)
    private String dimension;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(columnDefinition = "TEXT")
    private String rubric;

    @Column(columnDefinition = "TEXT")
    private String userAnswer;

    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private LocalDateTime answeredAt;

    public MockInterviewQuestion(int questionOrder, String dimension, String question, String rubric) {
        this.questionOrder = questionOrder;
        this.dimension = dimension;
        this.question = question;
        this.rubric = rubric;
    }
}
