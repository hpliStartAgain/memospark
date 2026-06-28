package com.memospark.core.service;

import com.memospark.core.domain.DeckLinkSource;
import com.memospark.core.domain.JobJd;
import com.memospark.core.domain.Target;
import com.memospark.core.domain.TargetSkill;
import com.memospark.core.domain.TargetStatus;
import com.memospark.core.domain.User;
import com.memospark.core.dto.*;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.JobJdRepository;
import com.memospark.core.repository.TargetRepository;
import com.memospark.core.repository.TargetSkillRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;
    private final JobJdRepository jobJdRepository;
    private final TargetSkillRepository targetSkillRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final ReadinessService readinessService;
    private final TargetSkillService targetSkillService;

    // ── Targets ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TargetSummaryDto> list(Long userId) {
        return targetRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(t -> toSummary(t, userId))
                .toList();
    }

    @Transactional
    public TargetDetailDto create(Long userId, CreateTargetRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        User user = userRepository.getReferenceById(userId);
        Target target = new Target(user, req.title().trim(), trimOrNull(req.company()));
        target.setStatus(parseStatus(req.status(), TargetStatus.PREPARING));
        target.setInterviewDate(req.interviewDate());
        target.setNotes(trimOrNull(req.notes()));
        target = targetRepository.save(target);
        return toDetail(target, userId);
    }

    @Transactional(readOnly = true)
    public TargetDetailDto getDetail(Long id, Long userId, boolean admin) {
        return toDetail(getOwned(id, userId, admin), userId);
    }

    @Transactional
    public TargetDetailDto update(Long id, Long userId, boolean admin, CreateTargetRequest req) {
        Target target = getOwned(id, userId, admin);
        if (req.title() != null && !req.title().isBlank()) target.setTitle(req.title().trim());
        target.setCompany(trimOrNull(req.company()));
        if (req.status() != null) target.setStatus(parseStatus(req.status(), target.getStatus()));
        target.setInterviewDate(req.interviewDate());
        target.setNotes(trimOrNull(req.notes()));
        targetRepository.save(target);
        return toDetail(target, userId);
    }

    @Transactional
    public TargetDetailDto updateStatus(Long id, Long userId, boolean admin, UpdateTargetStatusRequest req) {
        Target target = getOwned(id, userId, admin);
        if (req == null || req.status() == null || req.status().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        TargetStatus parsed = parseStatus(req.status(), null);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid target status");
        }
        target.setStatus(parsed);
        targetRepository.save(target);
        return toDetail(target, userId);
    }

    @Transactional
    public void delete(Long id, Long userId, boolean admin) {
        Target target = getOwned(id, userId, admin);
        targetSkillService.deleteSkillsAndDecks(id);
        targetRepository.delete(target);
    }

    // ── JDs ──────────────────────────────────────────────────────────────

    @Transactional
    public JobJdDto addJd(Long targetId, Long userId, boolean admin, CreateJobJdRequest req) {
        Target target = getOwned(targetId, userId, admin);
        if (req.content() == null || req.content().isBlank()) {
            throw new IllegalArgumentException("JD content is required");
        }
        JobJd jd = new JobJd(target, trimOrNull(req.title()), req.content().trim(), trimOrNull(req.source()));
        return toJdDto(jobJdRepository.save(jd));
    }

    @Transactional
    public void deleteJd(Long targetId, Long jdId, Long userId, boolean admin) {
        getOwned(targetId, userId, admin);
        JobJd jd = jobJdRepository.findById(jdId)
                .orElseThrow(() -> new NoSuchElementException("JD not found"));
        if (!jd.getTarget().getId().equals(targetId)) {
            throw new SecurityException("JD does not belong to this target");
        }
        jobJdRepository.delete(jd);
    }

    // ── Skill analysis & maintenance ─────────────────────────────────────

    @Transactional
    public TargetDetailDto analyze(Long targetId, Long userId, boolean admin, AnalyzeTargetRequest req) {
        Target target = getOwned(targetId, userId, admin);
        String language = (req != null && req.language() != null) ? req.language() : "zh";
        boolean replace = req != null && req.replace();
        targetSkillService.analyzeAndPersist(target, language, replace);
        return toDetail(target, userId);
    }

    @Transactional
    public TargetSkillDto addSkill(Long targetId, Long userId, boolean admin, CreateSkillRequest req) {
        Target target = getOwned(targetId, userId, admin);
        if (req.name() == null || req.name().isBlank()) {
            throw new IllegalArgumentException("Skill name is required");
        }
        int weight = clamp(req.weight() != null ? req.weight() : 3, 1, 5);
        TargetSkill skill = new TargetSkill(target, target.getUser(), req.name().trim(),
                trimOrNull(req.category()), trimOrNull(req.description()), weight);
        skill.setDeckLinkSource(DeckLinkSource.MANUAL);
        return toSkillDto(targetSkillRepository.save(skill));
    }

    @Transactional
    public TargetSkillDto updateSkill(Long targetId, Long skillId, Long userId, boolean admin, UpdateSkillRequest req) {
        getOwned(targetId, userId, admin);
        TargetSkill skill = getOwnedSkill(targetId, skillId);
        if (req.name() != null && !req.name().isBlank()) skill.setName(req.name().trim());
        if (req.category() != null) skill.setCategory(trimOrNull(req.category()));
        if (req.description() != null) skill.setDescription(trimOrNull(req.description()));
        if (req.weight() != null) skill.setWeight(clamp(req.weight(), 1, 5));
        if (req.selfLevel() != null) skill.setSelfLevel(clamp(req.selfLevel(), 0, 5));
        return toSkillDto(targetSkillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long targetId, Long skillId, Long userId, boolean admin) {
        getOwned(targetId, userId, admin);
        TargetSkill skill = getOwnedSkill(targetId, skillId);
        targetSkillService.deleteSkillWithDeck(skill);
    }

    @Transactional(readOnly = true)
    public ReadinessDto getReadiness(Long targetId, Long userId, boolean admin) {
        return readinessService.compute(getOwned(targetId, userId, admin), userId);
    }

    @Transactional
    public TargetSkillDto generateSkillCards(Long targetId, Long skillId, Long userId, boolean admin, String language) {
        getOwned(targetId, userId, admin);
        TargetSkill skill = getOwnedSkill(targetId, skillId);
        targetSkillService.generateCardsForSkill(skill, (language != null && !language.isBlank()) ? language : "zh");
        return toSkillDto(skill);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Target getOwned(Long id, Long userId, boolean admin) {
        Target target = targetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Target not found"));
        if (!admin && !target.getUser().getId().equals(userId)) {
            throw new SecurityException("Not your target");
        }
        return target;
    }

    private TargetSkill getOwnedSkill(Long targetId, Long skillId) {
        TargetSkill skill = targetSkillRepository.findById(skillId)
                .orElseThrow(() -> new NoSuchElementException("Skill not found"));
        if (!skill.getTarget().getId().equals(targetId)) {
            throw new SecurityException("Skill does not belong to this target");
        }
        return skill;
    }

    private TargetSummaryDto toSummary(Target t, Long userId) {
        ReadinessDto readiness = readinessService.compute(t, userId);
        return new TargetSummaryDto(
                t.getId(), t.getTitle(), t.getCompany(), t.getStatus().name(),
                t.getInterviewDate(), daysUntil(t.getInterviewDate()),
                jobJdRepository.countByTargetId(t.getId()),
                targetSkillRepository.countByTargetId(t.getId()),
                readiness.overall()
        );
    }

    private TargetDetailDto toDetail(Target t, Long userId) {
        List<JobJdDto> jds = jobJdRepository.findByTargetIdOrderByCreatedAtDesc(t.getId())
                .stream().map(this::toJdDto).toList();
        List<TargetSkillDto> skills = targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(t.getId())
                .stream().map(this::toSkillDto).toList();
        ReadinessDto readiness = readinessService.compute(t, userId);
        return new TargetDetailDto(
                t.getId(), t.getTitle(), t.getCompany(), t.getStatus().name(),
                t.getInterviewDate(), daysUntil(t.getInterviewDate()), t.getNotes(),
                jds, skills, readiness
        );
    }

    private JobJdDto toJdDto(JobJd jd) {
        return new JobJdDto(jd.getId(), jd.getTitle(), jd.getContent(), jd.getSource(), jd.getCreatedAt());
    }

    private TargetSkillDto toSkillDto(TargetSkill s) {
        Long deckId = s.getDeck() != null ? s.getDeck().getId() : null;
        long cardCount = deckId != null ? cardRepository.countByDeckId(deckId) : 0L;
        String deckName = s.getDeck() != null ? s.getDeck().getName() : null;
        return new TargetSkillDto(s.getId(), s.getName(), s.getCategory(),
                s.getDescription(), s.getWeight(), s.getSelfLevel(), deckId, cardCount,
                s.getDeckLinkSource() != null ? s.getDeckLinkSource().name() : null,
                deckName,
                s.getDeckMatchScore());
    }

    private Long daysUntil(LocalDate date) {
        return date != null ? ChronoUnit.DAYS.between(LocalDate.now(), date) : null;
    }

    private TargetStatus parseStatus(String status, TargetStatus fallback) {
        if (status == null || status.isBlank()) return fallback;
        try {
            return TargetStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
