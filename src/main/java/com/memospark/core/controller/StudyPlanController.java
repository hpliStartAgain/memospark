package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.StudyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class StudyPlanController {

    private final StudyPlanService studyPlanService;

    @GetMapping("/target/{targetId}")
    public ResponseEntity<StudyPlanDto> getTargetPlan(
            @PathVariable Long targetId,
            @CurrentUser UserPrincipal principal) {
        return ResponseEntity.of(
                studyPlanService.getActivePlan(targetId, principal.id(), principal.admin()));
    }

    @PostMapping("/target/{targetId}/generate")
    public StudyPlanDto generate(
            @PathVariable Long targetId,
            @RequestBody(required = false) GenerateStudyPlanRequest request,
            @CurrentUser UserPrincipal principal) {
        return studyPlanService.generate(
                targetId, principal.id(), principal.admin(), request);
    }

    @GetMapping("/today")
    public List<StudyPlanItemDto> today(@CurrentUser UserPrincipal principal) {
        return studyPlanService.getToday(principal.id());
    }

    @PatchMapping("/items/{itemId}")
    public StudyPlanItemDto updateItem(
            @PathVariable Long itemId,
            @RequestBody UpdateStudyPlanItemRequest request,
            @CurrentUser UserPrincipal principal) {
        return studyPlanService.updateItem(itemId, principal.id(), principal.admin(), request);
    }
}

