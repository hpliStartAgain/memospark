package com.memospark.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memospark.core.domain.*;
import com.memospark.core.dto.*;
import com.memospark.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyPlanService {

    private static final int EXECUTION_WEEKS = 4;
    private static final int STUDY_DAYS_PER_WEEK = 6;

    private final StudyPlanRepository studyPlanRepository;
    private final StudyPlanItemRepository studyPlanItemRepository;
    private final TargetRepository targetRepository;
    private final TargetSkillRepository targetSkillRepository;
    private final JobJdRepository jobJdRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Optional<StudyPlanDto> getActivePlan(Long targetId, Long userId, boolean admin) {
        Target target = getOwnedTarget(targetId, userId, admin);
        return studyPlanRepository
                .findFirstByTargetIdAndUserIdAndActiveTrueOrderByCreatedAtDesc(target.getId(), target.getUser().getId())
                .map(this::toDto);
    }

    @Transactional
    public StudyPlanDto generate(
            Long targetId, Long userId, boolean admin, GenerateStudyPlanRequest request) {
        Target target = getOwnedTarget(targetId, userId, admin);
        List<TargetSkill> skills = targetSkillRepository.findByTargetIdOrderByWeightDescIdAsc(targetId);
        List<TargetSkill> plannableSkills = skills.stream()
                .filter(skill -> skill.getDeck() != null && cardRepository.countByDeckId(skill.getDeck().getId()) > 0)
                .toList();
        if (plannableSkills.isEmpty()) {
            throw new IllegalArgumentException("请先分析 JD，并为至少一个技能准备卡片。");
        }

        LocalDate startDate = LocalDate.now();
        LocalDate targetDate = resolveTargetDate(target, request, startDate);
        int weeklyHours = clamp(request != null && request.weeklyHours() != null ? request.weeklyHours() : 7, 3, 40);
        String language = request != null && request.language() != null ? request.language() : "zh";

        String context = buildTargetContext(target, plannableSkills);
        Map<String, Object> blueprint;
        try {
            blueprint = aiService.generateStudyPlan(
                    context, weeklyHours, targetDate.toString(), language, target.getUser().getId());
        } catch (RuntimeException aiError) {
            blueprint = fallbackBlueprint(target, plannableSkills, weeklyHours, startDate, targetDate);
        }
        blueprint = normalizeBlueprint(blueprint, plannableSkills, weeklyHours, startDate, targetDate);

        studyPlanRepository.findByTargetIdAndUserIdAndActiveTrue(targetId, target.getUser().getId())
                .forEach(existing -> existing.setActive(false));

        StudyPlan plan = new StudyPlan();
        plan.setTarget(target);
        plan.setUser(target.getUser());
        plan.setStartDate(startDate);
        plan.setTargetDate(targetDate);
        plan.setWeeklyHours(weeklyHours);
        plan.setSummary(asString(blueprint.get("summary"), "已根据目标岗位生成滚动学习计划。"));
        plan.setStrategy(asString(blueprint.get("strategy"), "先补齐基础，再进入机制理解和实战表达。"));
        plan.setRoadmapJson(writeJson(blueprint));
        plan = studyPlanRepository.save(plan);

        materializeItems(plan, blueprint, plannableSkills);
        return toDto(plan);
    }

    @Transactional
    public StudyPlanItemDto updateItem(
            Long itemId, Long userId, boolean admin, UpdateStudyPlanItemRequest request) {
        StudyPlanItem item = studyPlanItemRepository.findById(itemId)
                .orElseThrow(() -> new NoSuchElementException("Plan item not found"));
        if (!admin && !item.getPlan().getUser().getId().equals(userId)) {
            throw new SecurityException("Not your study plan");
        }
        item.setCompletedAt(request.completed() ? LocalDateTime.now() : null);
        item = studyPlanItemRepository.save(item);
        return toItemDto(item);
    }

    @Transactional(readOnly = true)
    public List<StudyPlanItemDto> getToday(Long userId) {
        return studyPlanItemRepository
                .findActiveByUserAndDate(userId, LocalDate.now())
                .stream()
                .map(this::toItemDto)
                .toList();
    }

    private void materializeItems(
            StudyPlan plan, Map<String, Object> blueprint, List<TargetSkill> skills) {
        Map<Long, Deck> availableDecks = skills.stream()
                .map(TargetSkill::getDeck)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Deck::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> weeks = mapList(blueprint.get("weeks"));
        List<StudyPlanItem> items = new ArrayList<>();

        for (int weekIndex = 0; weekIndex < EXECUTION_WEEKS; weekIndex++) {
            Map<String, Object> week = weeks.get(weekIndex);
            int weekNumber = weekIndex + 1;
            LocalDate weekStart = plan.getStartDate().plusWeeks(weekIndex);
            String objective = asString(week.get("objective"), "完成本周学习与到期复习");
            LearningStage stage = parseStage(asString(week.get("stage"), "FOUNDATION"));
            List<Deck> focusDecks = longList(week.get("focusDeckIds")).stream()
                    .map(availableDecks::get)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (focusDecks.isEmpty()) {
                focusDecks = availableDecks.values().stream().limit(2).toList();
            }

            int newCards = clamp(asInt(week.get("newCards"), plan.getWeeklyHours() * 3), 4, 80);
            int reviews = clamp(asInt(week.get("reviews"), plan.getWeeklyHours() * 10), 12, 240);
            int practiceSessions = clamp(asInt(week.get("practiceSessions"), 1), 0, 3);

            for (int day = 0; day < STUDY_DAYS_PER_WEEK; day++) {
                LocalDate date = weekStart.plusDays(day);
                items.add(newItem(
                        plan, null, date, weekNumber, StudyPlanItemType.REVIEW, null,
                        "复习到期卡片", "先清空到期队列，再学习新内容。",
                        ceilDiv(reviews, STUDY_DAYS_PER_WEEK), day * 10));

                if (day < 4 && !focusDecks.isEmpty()) {
                    Deck deck = focusDecks.get(day % focusDecks.size());
                    items.add(newItem(
                            plan, deck, date, weekNumber, StudyPlanItemType.LEARN, stage,
                            "学习「" + deck.getName() + "」",
                            objective,
                            ceilDiv(newCards, 4), day * 10 + 1));
                }
            }

            if (practiceSessions > 0 && !focusDecks.isEmpty()) {
                Deck deck = focusDecks.get(0);
                items.add(newItem(
                        plan, deck, weekStart.plusDays(4), weekNumber,
                        StudyPlanItemType.PRACTICE, LearningStage.PRACTICE,
                        "完成情境练习", "用真实故障、设计题或面试表达检验本周知识。",
                        practiceSessions, 49));
            }
            items.add(newItem(
                    plan, null, weekStart.plusDays(5), weekNumber,
                    StudyPlanItemType.CHECKPOINT, stage,
                    "本周复盘", objective, 1, 59));
        }
        studyPlanItemRepository.saveAll(items);
    }

    private StudyPlanItem newItem(
            StudyPlan plan,
            Deck deck,
            LocalDate date,
            int weekNumber,
            StudyPlanItemType type,
            LearningStage stage,
            String title,
            String objective,
            int targetCount,
            int sortOrder) {
        StudyPlanItem item = new StudyPlanItem();
        item.setPlan(plan);
        item.setDeck(deck);
        item.setPlanDate(date);
        item.setWeekNumber(weekNumber);
        item.setItemType(type);
        item.setLearningStage(stage);
        item.setTitle(title);
        item.setObjective(objective);
        item.setTargetCount(targetCount);
        item.setSortOrder(sortOrder);
        return item;
    }

    private StudyPlanDto toDto(StudyPlan plan) {
        JsonNode roadmap = readJson(plan.getRoadmapJson());
        Map<Integer, JsonNode> weekMeta = new HashMap<>();
        JsonNode weeksNode = roadmap.path("weeks");
        if (weeksNode.isArray()) {
            weeksNode.forEach(node -> weekMeta.put(node.path("week").asInt(), node));
        }

        Map<Integer, List<StudyPlanItem>> byWeek = studyPlanItemRepository
                .findByPlanIdOrderByPlanDateAscSortOrderAsc(plan.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        StudyPlanItem::getWeekNumber, LinkedHashMap::new, Collectors.toList()));

        List<StudyPlanWeekDto> weeks = byWeek.entrySet().stream().map(entry -> {
            int weekNumber = entry.getKey();
            List<StudyPlanItem> weekItems = entry.getValue();
            JsonNode meta = weekMeta.get(weekNumber);
            Map<LocalDate, List<StudyPlanItemDto>> days = weekItems.stream()
                    .map(this::toItemDto)
                    .collect(Collectors.groupingBy(
                            StudyPlanItemDto::date, LinkedHashMap::new, Collectors.toList()));
            List<StudyPlanDayDto> dayDtos = days.entrySet().stream()
                    .map(day -> new StudyPlanDayDto(day.getKey(), day.getValue()))
                    .toList();
            LocalDate start = weekItems.get(0).getPlanDate();
            LocalDate end = weekItems.get(weekItems.size() - 1).getPlanDate();
            return new StudyPlanWeekDto(
                    weekNumber,
                    start,
                    end,
                    meta != null ? meta.path("objective").asText("") : "",
                    meta != null ? meta.path("stage").asText("") : "",
                    dayDtos);
        }).toList();

        return new StudyPlanDto(
                plan.getId(),
                plan.getTarget().getId(),
                plan.getTarget().getTitle(),
                plan.getStartDate(),
                plan.getTargetDate(),
                plan.getWeeklyHours(),
                plan.getSummary(),
                plan.getStrategy(),
                roadmap,
                weeks,
                plan.getCreatedAt());
    }

    private StudyPlanItemDto toItemDto(StudyPlanItem item) {
        long completedCount = switch (item.getItemType()) {
            case LEARN -> item.getDeck() == null || item.getLearningStage() == null
                    ? 0
                    : cardProgressRepository.countFirstLearnedByDeckIdAndDateAndStage(
                            item.getDeck().getId(), item.getPlanDate(), item.getLearningStage());
            case REVIEW -> item.getDeck() == null
                    ? reviewLogRepository.countByUserIdAndReviewDateAndLearningMode(
                            item.getPlan().getUser().getId(), item.getPlanDate(), "REVIEW")
                    : reviewLogRepository.countByDeckIdAndReviewDateAndLearningMode(
                            item.getDeck().getId(), item.getPlanDate(), "REVIEW");
            case PRACTICE, CHECKPOINT -> item.getCompletedAt() != null ? item.getTargetCount() : 0;
        };
        completedCount = Math.min(completedCount, item.getTargetCount());
        boolean completed = completedCount >= item.getTargetCount();
        return new StudyPlanItemDto(
                item.getId(),
                item.getPlanDate(),
                item.getItemType().name(),
                item.getDeck() != null ? item.getDeck().getId() : null,
                item.getDeck() != null ? item.getDeck().getName() : null,
                item.getLearningStage() != null ? item.getLearningStage().name() : null,
                item.getTitle(),
                item.getObjective(),
                item.getTargetCount(),
                completedCount,
                completed);
    }

    private String buildTargetContext(Target target, List<TargetSkill> skills) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("target", Map.of(
                "title", target.getTitle(),
                "company", target.getCompany() != null ? target.getCompany() : "",
                "interviewDate", target.getInterviewDate() != null ? target.getInterviewDate().toString() : ""));
        context.put("jobDescriptions", jobJdRepository.findByTargetIdOrderByCreatedAtDesc(target.getId()).stream()
                .map(jd -> truncate(jd.getContent(), 6000))
                .toList());
        context.put("skills", skills.stream().map(skill -> {
            Deck deck = skill.getDeck();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("name", skill.getName());
            value.put("weight", skill.getWeight());
            value.put("selfLevel", skill.getSelfLevel());
            value.put("deckId", deck.getId());
            value.put("deckName", deck.getName());
            value.put("cards", cardRepository.countByDeckId(deck.getId()));
            value.put("foundationCards", cardRepository.countByDeckIdAndLearningStage(
                    deck.getId(), LearningStage.FOUNDATION));
            value.put("advancedCards", cardRepository.countByDeckIdAndLearningStage(
                    deck.getId(), LearningStage.ADVANCED));
            value.put("practiceCards", cardRepository.countByDeckIdAndLearningStage(
                    deck.getId(), LearningStage.PRACTICE));
            return value;
        }).toList());
        return writeJson(context);
    }

    private Map<String, Object> fallbackBlueprint(
            Target target,
            List<TargetSkill> skills,
            int weeklyHours,
            LocalDate startDate,
            LocalDate targetDate) {
        int totalWeeks = Math.max(4, (int) Math.ceil(ChronoUnit.DAYS.between(startDate, targetDate) / 7.0));
        int foundationEnd = Math.max(1, totalWeeks / 3);
        int advancedEnd = Math.max(foundationEnd + 1, totalWeeks * 2 / 3);
        List<Long> deckIds = skills.stream().map(skill -> skill.getDeck().getId()).distinct().toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "围绕 " + target.getTitle() + " 的 JD 缺口，先建立基础，再训练机制理解与实战表达。");
        result.put("strategy", "每天先完成到期复习，再学习少量新卡；每周用一次情境练习和复盘校准下一周。");
        result.put("phases", List.of(
                Map.of("name", "基础覆盖", "startWeek", 1, "endWeek", foundationEnd,
                        "goal", "完成高权重技能的基础卡片并能准确复述核心概念"),
                Map.of("name", "机制进阶", "startWeek", foundationEnd + 1, "endWeek", advancedEnd,
                        "goal", "解释关键机制、取舍和常见故障路径"),
                Map.of("name", "面试实战", "startWeek", advancedEnd + 1, "endWeek", totalWeeks,
                        "goal", "稳定完成场景题、系统设计和面试表达")));
        List<Map<String, Object>> weeks = new ArrayList<>();
        for (int week = 1; week <= EXECUTION_WEEKS; week++) {
            weeks.add(Map.of(
                    "week", week,
                    "objective", "完成高权重技能的基础覆盖并保持到期复习清零",
                    "stage", "FOUNDATION",
                    "focusDeckIds", deckIds.stream().limit(2).toList(),
                    "newCards", weeklyHours * 3,
                    "reviews", weeklyHours * 10,
                    "practiceSessions", 1));
        }
        result.put("weeks", weeks);
        result.put("risks", List.of("若到期复习积压，暂停新增卡片，先恢复记忆负荷。"));
        return result;
    }

    private Map<String, Object> normalizeBlueprint(
            Map<String, Object> blueprint,
            List<TargetSkill> skills,
            int weeklyHours,
            LocalDate startDate,
            LocalDate targetDate) {
        Map<String, Object> fallback = fallbackBlueprint(
                skills.get(0).getTarget(), skills, weeklyHours, startDate, targetDate);
        Map<String, Object> normalized = new LinkedHashMap<>(blueprint != null ? blueprint : Map.of());
        normalized.putIfAbsent("summary", fallback.get("summary"));
        normalized.putIfAbsent("strategy", fallback.get("strategy"));
        normalized.putIfAbsent("phases", fallback.get("phases"));
        normalized.putIfAbsent("risks", fallback.get("risks"));

        List<Map<String, Object>> fallbackWeeks = mapList(fallback.get("weeks"));
        List<Map<String, Object>> providedWeeks = mapList(normalized.get("weeks"));
        List<Map<String, Object>> weeks = new ArrayList<>();
        for (int index = 0; index < EXECUTION_WEEKS; index++) {
            Map<String, Object> week = new LinkedHashMap<>(
                    index < providedWeeks.size() ? providedWeeks.get(index) : fallbackWeeks.get(index));
            week.put("week", index + 1);
            week.putIfAbsent("objective", fallbackWeeks.get(index).get("objective"));
            week.putIfAbsent("stage", fallbackWeeks.get(index).get("stage"));
            week.putIfAbsent("focusDeckIds", fallbackWeeks.get(index).get("focusDeckIds"));
            week.putIfAbsent("newCards", fallbackWeeks.get(index).get("newCards"));
            week.putIfAbsent("reviews", fallbackWeeks.get(index).get("reviews"));
            week.putIfAbsent("practiceSessions", fallbackWeeks.get(index).get("practiceSessions"));
            weeks.add(week);
        }
        normalized.put("weeks", weeks);
        return normalized;
    }

    private Target getOwnedTarget(Long targetId, Long userId, boolean admin) {
        Target target = targetRepository.findById(targetId)
                .orElseThrow(() -> new NoSuchElementException("Target not found"));
        if (!admin && !target.getUser().getId().equals(userId)) {
            throw new SecurityException("Not your target");
        }
        return target;
    }

    private LocalDate resolveTargetDate(
            Target target, GenerateStudyPlanRequest request, LocalDate startDate) {
        LocalDate requested = request != null ? request.targetDate() : null;
        LocalDate targetDate = requested != null ? requested : target.getInterviewDate();
        if (targetDate == null || !targetDate.isAfter(startDate)) {
            targetDate = startDate.plusYears(1);
        }
        return targetDate;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private List<Long> longList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .map(this::asLong)
                .filter(Objects::nonNull)
                .toList();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String asString(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private LearningStage parseStage(String value) {
        try {
            return LearningStage.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return LearningStage.FOUNDATION;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize study plan", e);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private int ceilDiv(int value, int divisor) {
        return Math.max(1, (value + divisor - 1) / divisor);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max) + "\n...[truncated]";
    }
}

