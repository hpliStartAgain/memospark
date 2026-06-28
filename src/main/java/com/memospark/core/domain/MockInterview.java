package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_interviews")
@Getter
@Setter
@NoArgsConstructor
public class MockInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private Target target;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MockInterviewType type = MockInterviewType.MIXED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MockInterviewStatus status = MockInterviewStatus.IN_PROGRESS;

    @Column(nullable = false)
    private int questionCount = 0;

    private Double averageScore;

    @Column(columnDefinition = "TEXT")
    private String summaryFeedback;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionOrder ASC")
    private List<MockInterviewQuestion> questions = new ArrayList<>();

    public MockInterview(Target target, User user, MockInterviewType type, int questionCount) {
        this.target = target;
        this.user = user;
        this.type = type;
        this.questionCount = questionCount;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addQuestion(MockInterviewQuestion question) {
        questions.add(question);
        question.setInterview(this);
    }
}
