package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.JobJdRepository;
import com.memospark.core.repository.TargetSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Turns the ephemeral JD analysis from {@link AiService} into persisted
 * {@link TargetSkill} rows (skill name + requirement weight + self level),
 * so that skill gaps can be tracked over time instead of being thrown away.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TargetSkillService {

    private static final int MIN_CARDS = 6;
    private static final int MAX_CARDS = 20;
    private static final int DEFAULT_CARDS = 10;

    private final AiService aiService;
    private final JobJdRepository jobJdRepository;
    private final TargetSkillRepository targetSkillRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final SpacedRepetitionService spacedRepetitionService;
    private final DeckService deckService;

    @Transactional
    public List<TargetSkill> analyzeAndPersist(Target target, String language, boolean replace) {
        List<JobJd> jds = jobJdRepository.findByTargetIdOrderByCreatedAtDesc(target.getId());
        if (jds.isEmpty()) {
            throw new IllegalArgumentException("No JDs to analyze. Add at least one job description first.");
        }

        List<String> contents = jds.stream()
                .map(JobJd::getContent)
                .filter(c -> c != null && !c.isBlank())
                .toList();
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("All JDs are empty.");
        }

        Map<String, Object> analysis = aiService.analyzeJds(contents, language, target.getUser().getId());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> decks = (List<Map<String, Object>>) analysis.getOrDefault("decks", List.of());
        if (decks.isEmpty()) {
            throw new IllegalStateException("AI returned no skill areas for these JDs.");
        }

        if (replace) {
            deleteSkillsAndDecks(target.getId());
        }

        List<TargetSkill> saved = new ArrayList<>();
        for (Map<String, Object> deck : decks) {
            String name = asString(deck.get("name"));
            if (name == null || name.isBlank()) continue;

            String description = buildDescription(deck);
            String topics = joinTopics(deck.get("topics"));
            int suggested = clampCount(asInt(deck.get("suggestedCardCount"), DEFAULT_CARDS));
            int weight = mapWeight(deck.get("suggestedCardCount"));

            // Create the (empty) study deck this skill maps to. Cards are generated
            // on demand (one AI call per skill) to keep this request fast and AI cost bounded.
            Deck studyDeck = new Deck(name, description, DeckType.CUSTOM, target.getUser());
            studyDeck.setDailyNewCardLimit(20);
            studyDeck = deckRepository.save(studyDeck);

            TargetSkill skill = new TargetSkill(target, target.getUser(), name, null, description, weight);
            skill.setDeck(studyDeck);
            skill.setTopics(topics);
            skill.setSuggestedCardCount(suggested);
            saved.add(targetSkillRepository.save(skill));
        }
        return saved;
    }

    /**
     * Generate flashcards on demand for a single skill, filling its study deck.
     * One AI call per skill keeps latency and cost bounded (chosen over bulk
     * synchronous generation, which risks request timeouts).
     */
    @Transactional
    public int generateCardsForSkill(TargetSkill skill, String language) {
        Deck deck = skill.getDeck();
        if (deck == null) {
            deck = new Deck(skill.getName(), skill.getDescription(), DeckType.CUSTOM, skill.getUser());
            deck.setDailyNewCardLimit(20);
            deck = deckRepository.save(deck);
            skill.setDeck(deck);
            targetSkillRepository.save(skill);
        }

        int count = clampCount(skill.getSuggestedCardCount() > 0 ? skill.getSuggestedCardCount() : DEFAULT_CARDS);
        String topic = (skill.getTopics() != null && !skill.getTopics().isBlank())
                ? skill.getTopics().replace("\n", ", ")
                : (skill.getDescription() != null && !skill.getDescription().isBlank())
                    ? skill.getDescription()
                    : skill.getName();

        List<Map<String, String>> cards = aiService.generateCardsForTopic(
                skill.getName(), topic, count, language, skill.getUser().getId());

        Long userId = skill.getUser().getId();
        int created = 0;
        for (Map<String, String> c : cards) {
            String front = c.get("front");
            String back = c.get("back");
            if (front == null || front.isBlank() || back == null || back.isBlank()) continue;
            Card card = new Card(deck, front.trim(), back.trim(), c.get("tags"));
            card = cardRepository.save(card);
            CardProgress progress = new CardProgress(card);
            spacedRepetitionService.initProgress(progress, userId);
            cardProgressRepository.save(progress);
            created++;
        }
        return created;
    }

    /** Delete a single skill together with its generated study deck (cards + progress + logs). */
    @Transactional
    public void deleteSkillWithDeck(TargetSkill skill) {
        Deck deck = skill.getDeck();
        targetSkillRepository.delete(skill);
        if (deck != null) {
            deckService.deleteDeck(deck.getId());
        }
    }

    /** Delete every skill of a target plus their generated decks (used by replace + target deletion). */
    @Transactional
    public void deleteSkillsAndDecks(Long targetId) {
        for (TargetSkill skill : targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(targetId)) {
            deleteSkillWithDeck(skill);
        }
    }

    private String buildDescription(Map<String, Object> deck) {
        StringBuilder sb = new StringBuilder();
        String desc = asString(deck.get("description"));
        if (desc != null && !desc.isBlank()) sb.append(desc.trim());

        Object topicsObj = deck.get("topics");
        if (topicsObj instanceof List<?> topics && !topics.isEmpty()) {
            String joined = topics.stream().map(String::valueOf).reduce((a, b) -> a + " · " + b).orElse("");
            if (!joined.isBlank()) {
                if (sb.length() > 0) sb.append(" — ");
                sb.append(joined);
            }
        }
        String result = sb.toString();
        return result.length() > 1000 ? result.substring(0, 1000) : result;
    }

    private int mapWeight(Object suggestedCardCount) {
        if (!(suggestedCardCount instanceof Number n)) return 3;
        int count = n.intValue();
        if (count >= 18) return 5;
        if (count >= 14) return 4;
        if (count >= 10) return 3;
        if (count >= 6) return 2;
        return 1;
    }

    private String joinTopics(Object topicsObj) {
        if (topicsObj instanceof List<?> topics && !topics.isEmpty()) {
            String joined = topics.stream()
                    .map(String::valueOf)
                    .filter(s -> s != null && !s.isBlank())
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
            return joined.length() > 2000 ? joined.substring(0, 2000) : joined;
        }
        return null;
    }

    private int clampCount(int n) {
        return Math.max(MIN_CARDS, Math.min(MAX_CARDS, n));
    }

    private int asInt(Object o, int fallback) {
        if (o instanceof Number n) return n.intValue();
        if (o != null) {
            try {
                return Integer.parseInt(o.toString().trim());
            } catch (NumberFormatException ignored) {
                // fall through to fallback
            }
        }
        return fallback;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
