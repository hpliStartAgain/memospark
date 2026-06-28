package com.memospark.core.service;

import com.memospark.core.domain.BookmarkType;
import com.memospark.core.domain.MockInterviewStatus;
import com.memospark.core.domain.Target;
import com.memospark.core.domain.TargetSkill;
import com.memospark.core.dto.ReadinessDto;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.MockInterviewRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.ReviewLogRepository;
import com.memospark.core.repository.TargetSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes an interview-readiness score for a target.
 * MVP heuristic combining three signals:
 *   - skillCoverage: weighted mastery of target skills, blended with review evidence when available
 *   - cardHealth:    how few flashcards are overdue (review discipline)
 *   - wrongClear:    how few wrong-problem notes are still due for retry
 *   - mockPerformance: recent mock-interview score evidence
 */
@Service
@RequiredArgsConstructor
public class ReadinessService {

    private static final int WEAK_LEVEL_THRESHOLD = 3;
    private static final int EVIDENCE_WINDOW_DAYS = 30;
    private static final int MOCK_WINDOW_DAYS = 60;
    private static final double SELF_LEVEL_WEIGHT_WITH_EVIDENCE = 0.5;

    private final TargetSkillRepository targetSkillRepository;
    private final CardProgressRepository cardProgressRepository;
    private final CardRepository cardRepository;
    private final ProblemNoteRepository problemNoteRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final MockInterviewRepository mockInterviewRepository;

    public ReadinessDto compute(Target target, Long userId) {
        LocalDate today = LocalDate.now();

        // ── Skill coverage (weighted, evidence-aware) ──
        List<TargetSkill> skills = targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(target.getId());
        LocalDate evidenceSince = today.minusDays(EVIDENCE_WINDOW_DAYS);
        double weightedScore = 0;
        int weightedMax = 0;
        long weakSkills = 0;
        for (TargetSkill s : skills) {
            int w = Math.max(1, s.getWeight());
            double level = evidenceAwareSkillLevel(s, evidenceSince);
            weightedScore += w * level;
            weightedMax += w * 5;
            if (level < WEAK_LEVEL_THRESHOLD) weakSkills++;
        }
        int skillCoverage = weightedMax > 0 ? Math.round((float) (weightedScore * 100 / weightedMax)) : 0;

        // ── Card health ──
        long dueCards = cardProgressRepository.countDueCardsByUserId(userId, today);
        long totalCards = cardRepository.countByUserId(userId);
        int cardHealth = totalCards > 0
                ? Math.round((1f - Math.min(dueCards, totalCards) / (float) totalCards) * 100f)
                : 0;

        // ── Wrong-note clearance ──
        long wrongTotal = problemNoteRepository.countByUserIdAndBookmarkType(userId, BookmarkType.WRONG);
        long dueNotes = problemNoteRepository.findByUserIdAndNextRetryDateLessThanEqual(userId, today).size();
        int wrongClear = wrongTotal > 0
                ? Math.round((1f - Math.min(dueNotes, wrongTotal) / (float) wrongTotal) * 100f)
                : 100;

        // ── Mock interview performance ──
        LocalDateTime mockSince = today.minusDays(MOCK_WINDOW_DAYS).atStartOfDay();
        long mockCount = mockInterviewRepository.countByTargetIdAndStatusAndFinishedAtGreaterThanEqual(
                target.getId(), MockInterviewStatus.FINISHED, mockSince);
        int mockPerformance = mockCount > 0
                ? Math.round((float) mockInterviewRepository.calculateAverageScoreByTargetIdSince(
                        target.getId(), MockInterviewStatus.FINISHED, mockSince))
                : 0;

        int overall = mockCount > 0
                ? Math.round(skillCoverage * 0.4f + cardHealth * 0.25f + wrongClear * 0.15f + mockPerformance * 0.2f)
                : Math.round(skillCoverage * 0.5f + cardHealth * 0.3f + wrongClear * 0.2f);

        Long daysUntil = target.getInterviewDate() != null
                ? ChronoUnit.DAYS.between(today, target.getInterviewDate())
                : null;

        return new ReadinessDto(overall, skillCoverage, cardHealth, wrongClear, mockPerformance,
                dueCards, dueNotes, weakSkills, daysUntil);
    }

    private double evidenceAwareSkillLevel(TargetSkill skill, LocalDate since) {
        double selfLevel = Math.min(5, Math.max(0, skill.getSelfLevel()));
        if (skill.getDeck() == null || skill.getDeck().getId() == null) {
            return selfLevel;
        }

        Long deckId = skill.getDeck().getId();
        long evidenceCount = reviewLogRepository.countByDeckIdSince(deckId, since);
        if (evidenceCount <= 0) {
            return selfLevel;
        }

        double evidenceLevel = reviewLogRepository.calculateRetentionRateByDeckIdSince(deckId, since) * 5.0;
        return selfLevel * SELF_LEVEL_WEIGHT_WITH_EVIDENCE
                + evidenceLevel * (1.0 - SELF_LEVEL_WEIGHT_WITH_EVIDENCE);
    }
}
