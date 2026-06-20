package com.memospark.core.service;

import com.memospark.core.domain.BookmarkType;
import com.memospark.core.domain.Target;
import com.memospark.core.domain.TargetSkill;
import com.memospark.core.dto.ReadinessDto;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.TargetSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes an interview-readiness score for a target.
 * MVP heuristic combining three signals:
 *   - skillCoverage: weighted self-assessed mastery of the target's required skills
 *   - cardHealth:    how few flashcards are overdue (review discipline)
 *   - wrongClear:    how few wrong-problem notes are still due for retry
 */
@Service
@RequiredArgsConstructor
public class ReadinessService {

    private static final int WEAK_LEVEL_THRESHOLD = 3;

    private final TargetSkillRepository targetSkillRepository;
    private final CardProgressRepository cardProgressRepository;
    private final CardRepository cardRepository;
    private final ProblemNoteRepository problemNoteRepository;

    public ReadinessDto compute(Target target, Long userId) {
        LocalDate today = LocalDate.now();

        // ── Skill coverage (weighted) ──
        List<TargetSkill> skills = targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(target.getId());
        int weightedScore = 0;
        int weightedMax = 0;
        long weakSkills = 0;
        for (TargetSkill s : skills) {
            int w = Math.max(1, s.getWeight());
            weightedScore += w * Math.min(5, Math.max(0, s.getSelfLevel()));
            weightedMax += w * 5;
            if (s.getSelfLevel() < WEAK_LEVEL_THRESHOLD) weakSkills++;
        }
        int skillCoverage = weightedMax > 0 ? Math.round(weightedScore * 100f / weightedMax) : 0;

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

        int overall = Math.round(skillCoverage * 0.5f + cardHealth * 0.3f + wrongClear * 0.2f);

        Long daysUntil = target.getInterviewDate() != null
                ? ChronoUnit.DAYS.between(today, target.getInterviewDate())
                : null;

        return new ReadinessDto(overall, skillCoverage, cardHealth, wrongClear,
                dueCards, dueNotes, weakSkills, daysUntil);
    }
}
