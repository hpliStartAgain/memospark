package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping("/target/{targetId}")
    public ResponseEntity<StudyPlanDto> getTargetPlan(
            @PathVariable Long targetId,
            @CurrentUser UserPrincipal principal) {
        log.info("Get plan for target {} by user {}", targetId, principal.id());
        return ResponseEntity.of(
                studyPlanService.getActivePlan(targetId, principal.id(), principal.admin()));
    }

    @PostMapping("/target/{targetId}/generate")
    public StudyPlanDto generate(
            @PathVariable Long targetId,
            @RequestBody(required = false) GenerateStudyPlanRequest request,
            @CurrentUser UserPrincipal principal) {
        log.info("Generate plan for target {} by user {} (weeklyHours={}, targetDate={})",
                targetId, principal.id(),
                request != null ? request.weeklyHours() : null,
                request != null ? request.targetDate() : null);
        StudyPlanDto result = studyPlanService.generate(
                targetId, principal.id(), principal.admin(), request);
        log.info("Plan generated: id={}, target={}, weeks={}",
                result.id(), result.targetId(), result.weeks().size());
        return result;
    }

    @GetMapping("/today")
    public List<StudyPlanItemDto> today(@CurrentUser UserPrincipal principal) {
        log.debug("Get today's plan items for user {}", principal.id());
        return studyPlanService.getToday(principal.id());
    }

    @PatchMapping("/items/{itemId}")
    public StudyPlanItemDto updateItem(
            @PathVariable Long itemId,
            @RequestBody UpdateStudyPlanItemRequest request,
            @CurrentUser UserPrincipal principal) {
        log.info("Update plan item {} by user {} (completed={})",
                itemId, principal.id(), request.completed());
        return studyPlanService.updateItem(itemId, principal.id(), principal.admin(), request);
    }
}

