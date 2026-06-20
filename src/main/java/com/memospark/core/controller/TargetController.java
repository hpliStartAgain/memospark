package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.TargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @GetMapping
    public List<TargetSummaryDto> list(@CurrentUser UserPrincipal principal) {
        return targetService.list(principal.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TargetDetailDto create(@RequestBody CreateTargetRequest req,
                                  @CurrentUser UserPrincipal principal) {
        return targetService.create(principal.id(), req);
    }

    @GetMapping("/{id}")
    public TargetDetailDto get(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        return targetService.getDetail(id, principal.id(), principal.admin());
    }

    @PutMapping("/{id}")
    public TargetDetailDto update(@PathVariable Long id, @RequestBody CreateTargetRequest req,
                                  @CurrentUser UserPrincipal principal) {
        return targetService.update(id, principal.id(), principal.admin(), req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        targetService.delete(id, principal.id(), principal.admin());
    }

    // ── JDs ──

    @PostMapping("/{id}/jds")
    @ResponseStatus(HttpStatus.CREATED)
    public JobJdDto addJd(@PathVariable Long id, @RequestBody CreateJobJdRequest req,
                          @CurrentUser UserPrincipal principal) {
        return targetService.addJd(id, principal.id(), principal.admin(), req);
    }

    @DeleteMapping("/{id}/jds/{jdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJd(@PathVariable Long id, @PathVariable Long jdId,
                         @CurrentUser UserPrincipal principal) {
        targetService.deleteJd(id, jdId, principal.id(), principal.admin());
    }

    // ── Skill analysis & maintenance ──

    @PostMapping("/{id}/analyze")
    public TargetDetailDto analyze(@PathVariable Long id, @RequestBody(required = false) AnalyzeTargetRequest req,
                                   @CurrentUser UserPrincipal principal) {
        return targetService.analyze(id, principal.id(), principal.admin(), req);
    }

    @PostMapping("/{id}/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public TargetSkillDto addSkill(@PathVariable Long id, @RequestBody CreateSkillRequest req,
                                   @CurrentUser UserPrincipal principal) {
        return targetService.addSkill(id, principal.id(), principal.admin(), req);
    }

    @PutMapping("/{id}/skills/{skillId}")
    public TargetSkillDto updateSkill(@PathVariable Long id, @PathVariable Long skillId,
                                      @RequestBody UpdateSkillRequest req,
                                      @CurrentUser UserPrincipal principal) {
        return targetService.updateSkill(id, skillId, principal.id(), principal.admin(), req);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable Long id, @PathVariable Long skillId,
                            @CurrentUser UserPrincipal principal) {
        targetService.deleteSkill(id, skillId, principal.id(), principal.admin());
    }

    @GetMapping("/{id}/readiness")
    public ReadinessDto readiness(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        return targetService.getReadiness(id, principal.id(), principal.admin());
    }
}
