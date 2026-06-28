package com.memospark.core.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "srs_settings")
@Getter
@Setter
@NoArgsConstructor
public class SrsSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private double initialEaseFactor = 2.5;

    @Column(nullable = false)
    private double minEaseFactor = 1.3;

    @Column(nullable = false)
    private int firstInterval = 1;

    @Column(nullable = false)
    private int secondInterval = 6;

    @Column(nullable = false)
    private double desiredRetention = 0.9;

    public SrsSettings(User user) {
        this.user = user;
    }
}
