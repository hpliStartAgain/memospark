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
import java.util.Comparator;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int MAX_CARDS = 40;
    private static final int DEFAULT_CARDS = 10;
    private static final double DECK_MATCH_THRESHOLD = 0.72;
    private static final int CARD_SAMPLE_LIMIT = 12;
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[a-z0-9+#.]+|[\\p{IsHan}]+");
    private static final Set<String> BROAD_TECH_TOKENS = Set.of(
            "java", "http", "https", "tcp", "udp", "sql", "api", "web", "rpc"
    );

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

        List<Deck> candidateDecks = deckRepository.findByUserId(target.getUser().getId()).stream()
                .filter(d -> d.getType() == DeckType.CUSTOM)
                .toList();

        List<TargetSkill> saved = new ArrayList<>();
        for (Map<String, Object> deck : decks) {
            String name = asString(deck.get("name"));
            if (name == null || name.isBlank()) continue;

            String description = buildDescription(deck);
            String topics = joinTopics(deck.get("topics"));
            int suggested = clampCount(asInt(deck.get("suggestedCardCount"), DEFAULT_CARDS));
            int weight = mapWeight(deck.get("suggestedCardCount"));

            DeckMatch match = findBestDeckMatch(candidateDecks, name, description, topics);
            Deck studyDeck;
            DeckLinkSource linkSource;
            Double matchScore = null;
            if (match.score() >= DECK_MATCH_THRESHOLD) {
                studyDeck = match.deck();
                linkSource = DeckLinkSource.MATCHED_EXISTING;
                matchScore = roundScore(match.score());
            } else {
                // Create an empty study deck only when the user's existing deck set
                // does not already cover this JD skill with high confidence.
                studyDeck = new Deck(name, description, DeckType.CUSTOM, target.getUser());
                studyDeck.setDailyNewCardLimit(20);
                studyDeck = deckRepository.save(studyDeck);
                candidateDecks = appendCandidate(candidateDecks, studyDeck);
                linkSource = DeckLinkSource.AI_CREATED;
            }

            TargetSkill skill = new TargetSkill(target, target.getUser(), name, null, description, weight);
            skill.setDeck(studyDeck);
            skill.setDeckLinkSource(linkSource);
            skill.setDeckMatchScore(matchScore);
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
        return generateCardsForSkill(skill, language, null);
    }

    /**
     * Streaming version: AI text chunks are forwarded to onChunk in real-time.
     * The AI call runs outside a transaction; only deck prep and card saving are transactional.
     */
    public int generateCardsForSkill(TargetSkill skill, String language, java.util.function.Consumer<String> onChunk) {
        // Phase 1: prepare deck and count (transactional)
        PrepareResult prep = prepareDeck(skill);
        if (prep.count <= 0) {
            return 0;
        }

        // Phase 2: AI call with streaming (no transaction — can take 30-60s)
        if (onChunk != null) onChunk.accept(null); // signal: AI call starting
        List<Map<String, String>> cards = aiService.generateCardsForTopic(
                skill.getName(), prep.topic, prep.count, language, skill.getUser().getId(), onChunk);

        // Phase 3: save cards (transactional)
        return saveCards(prep.deck, prep.existingCount, cards, skill.getUser().getId());
    }

    private record PrepareResult(Deck deck, String topic, int count, int existingCount) {}

    @Transactional
    public PrepareResult prepareDeck(TargetSkill skill) {
        Deck deck = skill.getDeck();
        if (deck == null) {
            deck = new Deck(skill.getName(), skill.getDescription(), DeckType.CUSTOM, skill.getUser());
            deck.setDailyNewCardLimit(20);
            deck = deckRepository.save(deck);
            skill.setDeck(deck);
            skill.setDeckLinkSource(DeckLinkSource.AI_CREATED);
            skill.setDeckMatchScore(null);
            targetSkillRepository.save(skill);
        }

        int desiredCount = clampCount(Math.max(
                skill.getSuggestedCardCount() > 0 ? skill.getSuggestedCardCount() : DEFAULT_CARDS,
                targetCardCount(skill.getWeight())));
        int existingCount = (int) Math.min(Integer.MAX_VALUE, cardRepository.countByDeckId(deck.getId()));
        int count = desiredCount - existingCount;
        String topic = (skill.getTopics() != null && !skill.getTopics().isBlank())
                ? skill.getTopics().replace("\n", ", ")
                : (skill.getDescription() != null && !skill.getDescription().isBlank())
                    ? skill.getDescription()
                    : skill.getName();
        return new PrepareResult(deck, topic, count, existingCount);
    }

    @Transactional
    public int saveCards(Deck deck, int existingCount, List<Map<String, String>> cards, Long userId) {
        Set<String> existingFronts = cardRepository.findByDeckId(deck.getId()).stream()
                .map(Card::getFront)
                .filter(front -> front != null && !front.isBlank())
                .map(front -> front.trim().toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        int created = 0;
        for (Map<String, String> c : cards) {
            String front = c.get("front");
            String back = c.get("back");
            if (front == null || front.isBlank() || back == null || back.isBlank()) continue;
            String normalizedFront = front.trim().toLowerCase(Locale.ROOT);
            if (!existingFronts.add(normalizedFront)) continue;
            Card card = new Card(deck, front.trim(), back.trim(), c.get("tags"));
            card.setContentDifficulty(parseDifficulty(c.get("difficulty")));
            card.setLearningStage(parseStage(c.get("stage")));
            card.setStageOrder(Math.max(1, asInt(c.get("order"), existingCount + created + 1)));
            card.setGovernanceNote("根据目标 JD 生成并完成初始分级");
            card.setGovernedAt(LocalDateTime.now());
            card = cardRepository.save(card);
            CardProgress progress = new CardProgress(card);
            spacedRepetitionService.initProgress(progress, userId);
            cardProgressRepository.save(progress);
            created++;
        }
        return created;
    }

    private int targetCardCount(int weight) {
        if (weight >= 5) return 32;
        if (weight >= 4) return 26;
        return 22;
    }

    private CardDifficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) return CardDifficulty.MEDIUM;
        try {
            return CardDifficulty.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return CardDifficulty.MEDIUM;
        }
    }

    private LearningStage parseStage(String value) {
        if (value == null || value.isBlank()) return LearningStage.FOUNDATION;
        try {
            return LearningStage.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return LearningStage.FOUNDATION;
        }
    }

    /** Delete a single skill together with its generated study deck (cards + progress + logs). */
    @Transactional
    public void deleteSkillWithDeck(TargetSkill skill) {
        Deck deck = skill.getDeck();
        targetSkillRepository.delete(skill);
        if (deck != null && skill.getDeckLinkSource() == DeckLinkSource.AI_CREATED) {
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

    private DeckMatch findBestDeckMatch(List<Deck> candidates, String name, String description, String topics) {
        if (candidates.isEmpty()) {
            return new DeckMatch(null, 0.0);
        }
        String desiredCorpus = joinNonBlank(name, description, topics);
        Set<String> desiredTokens = tokens(desiredCorpus);
        Set<String> desiredNameTokens = tokens(name);
        return candidates.stream()
                .map(deck -> new DeckMatch(deck, scoreDeck(deck, name, desiredTokens, desiredNameTokens)))
                .max(Comparator.comparingDouble(DeckMatch::score))
                .orElse(new DeckMatch(null, 0.0));
    }

    private double scoreDeck(Deck deck, String desiredName,
                             Set<String> desiredTokens, Set<String> desiredNameTokens) {
        Set<String> deckNameTokens = tokens(deck.getName());
        double nameScore = similarity(desiredNameTokens, deckNameTokens,
                normalizeComparable(desiredName), normalizeComparable(deck.getName()));
        if (hasSharedStrongNameToken(desiredNameTokens, deckNameTokens)) {
            nameScore = Math.max(nameScore, 0.90);
        }

        Set<String> deckDescriptionTokens = tokens(deck.getDescription());
        double descriptionScore = similarity(desiredTokens, deckDescriptionTokens, "", "");

        Set<String> tagTokens = tokens(String.join(" ", cardRepository.findDistinctTagsByDeckId(deck.getId())));
        double tagScore = similarity(desiredTokens, tagTokens, "", "");

        String cardSample = cardRepository.findByDeckId(deck.getId()).stream()
                .limit(CARD_SAMPLE_LIMIT)
                .map(c -> joinNonBlank(c.getFront(), c.getBack(), c.getTags()))
                .reduce("", (a, b) -> a + " " + b);
        double cardScore = similarity(desiredTokens, tokens(cardSample), "", "");

        double score = nameScore * 0.50
                + descriptionScore * 0.25
                + tagScore * 0.12
                + cardScore * 0.13;

        if (nameScore >= 0.90) {
            score = Math.max(score, 0.73);
        }
        if (nameScore >= 0.90 && (descriptionScore >= 0.08 || tagScore >= 0.08 || cardScore >= 0.08)) {
            score = Math.max(score, 0.78);
        }
        return Math.min(1.0, score);
    }

    private boolean hasSharedStrongNameToken(Set<String> a, Set<String> b) {
        for (String token : a) {
            if (b.contains(token)
                    && token.length() >= 5
                    && !BROAD_TECH_TOKENS.contains(token)
                    && token.chars().anyMatch(ch -> ch < 128 && Character.isLetterOrDigit(ch))) {
                return true;
            }
        }
        return false;
    }

    private double similarity(Set<String> a, Set<String> b, String normalizedA, String normalizedB) {
        if (normalizedA != null && !normalizedA.isBlank()
                && normalizedB != null && !normalizedB.isBlank()) {
            if (normalizedA.equals(normalizedB)) {
                return 1.0;
            }
            int minLength = Math.min(normalizedA.length(), normalizedB.length());
            if (minLength >= 4 && (normalizedA.contains(normalizedB) || normalizedB.contains(normalizedA))) {
                return 0.92;
            }
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        if (intersection.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        double jaccard = (double) intersection.size() / union.size();
        double coverage = (double) intersection.size() / Math.min(a.size(), b.size());
        return Math.max(jaccard, coverage * 0.85);
    }

    private Set<String> tokens(String text) {
        Set<String> result = new HashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(normalizeTerms(text));
        while (matcher.find()) {
            String token = matcher.group();
            if (token.chars().allMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN)) {
                addChineseTokens(result, token);
            } else if (token.length() >= 2) {
                result.add(token);
            }
        }
        return result;
    }

    private void addChineseTokens(Set<String> result, String token) {
        if (token.length() < 2) {
            return;
        }
        result.add(token);
        if (token.length() >= 4) {
            for (int i = 0; i <= token.length() - 2; i++) {
                result.add(token.substring(i, i + 2));
            }
            for (int i = 0; i <= token.length() - 3; i++) {
                result.add(token.substring(i, i + 3));
            }
        }
    }

    private String normalizeTerms(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace("k8s", "kubernetes")
                .replace("ci/cd", "cicd")
                .replace("devops", "devops")
                .replace("golang", "go");
    }

    private String normalizeComparable(String text) {
        return normalizeTerms(text).replaceAll("[^a-z0-9\\p{IsHan}]+", "");
    }

    private List<Deck> appendCandidate(List<Deck> candidates, Deck deck) {
        List<Deck> updated = new ArrayList<>(candidates);
        updated.add(deck);
        return updated;
    }

    private String joinNonBlank(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(value);
            }
        }
        return sb.toString();
    }

    private double roundScore(double score) {
        return Math.round(score * 100.0) / 100.0;
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

    private record DeckMatch(Deck deck, double score) {}
}
