package com.memospark.core.service;

import com.memospark.core.domain.JobJd;
import com.memospark.core.domain.Target;
import com.memospark.core.domain.TargetSkill;
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

    private final AiService aiService;
    private final JobJdRepository jobJdRepository;
    private final TargetSkillRepository targetSkillRepository;

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

        Map<String, Object> analysis = aiService.analyzeJds(contents, language);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> decks = (List<Map<String, Object>>) analysis.getOrDefault("decks", List.of());
        if (decks.isEmpty()) {
            throw new IllegalStateException("AI returned no skill areas for these JDs.");
        }

        if (replace) {
            targetSkillRepository.deleteByTargetId(target.getId());
        }

        List<TargetSkill> saved = new ArrayList<>();
        for (Map<String, Object> deck : decks) {
            String name = asString(deck.get("name"));
            if (name == null || name.isBlank()) continue;

            String description = buildDescription(deck);
            int weight = mapWeight(deck.get("suggestedCardCount"));

            TargetSkill skill = new TargetSkill(target, target.getUser(), name, null, description, weight);
            saved.add(targetSkillRepository.save(skill));
        }
        return saved;
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

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
