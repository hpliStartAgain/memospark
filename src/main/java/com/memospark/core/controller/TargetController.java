package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.TargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
@Slf4j
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

    @PatchMapping("/{id}/status")
    public TargetDetailDto updateStatus(@PathVariable Long id,
                                        @RequestBody UpdateTargetStatusRequest req,
                                        @CurrentUser UserPrincipal principal) {
        return targetService.updateStatus(id, principal.id(), principal.admin(), req);
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

    @PostMapping("/{id}/skills/{skillId}/generate-cards")
    public TargetSkillDto generateSkillCards(@PathVariable Long id, @PathVariable Long skillId,
                                             @RequestParam(defaultValue = "zh") String lang,
                                             @CurrentUser UserPrincipal principal) {
        return targetService.generateSkillCards(id, skillId, principal.id(), principal.admin(), lang);
    }

    /**
     * SSE streaming version of generate-cards.
     * Events: status | chunk | complete | error
     */
    @GetMapping(value = "/{id}/skills/{skillId}/generate-cards/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSkillCardsStream(@PathVariable Long id, @PathVariable Long skillId,
                                                @RequestParam(defaultValue = "zh") String lang,
                                                @CurrentUser UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 min timeout
        Thread.startVirtualThread(() -> {
            try {
                emitter.send(SseEmitter.event().name("status").data("正在准备..."));
                int created = targetService.generateSkillCardsStream(
                        id, skillId, principal.id(), principal.admin(), lang,
                        chunk -> {
                            try {
                                if (chunk == null) {
                                    emitter.send(SseEmitter.event().name("status").data("AI 正在生成卡片..."));
                                } else {
                                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                                }
                            } catch (Exception e) {
                                log.debug("SSE send chunk failed", e);
                            }
                        });
                emitter.send(SseEmitter.event().name("status").data("正在保存卡片..."));
                emitter.send(SseEmitter.event().name("complete").data("{\"created\":" + created + "}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("Streaming generate-cards failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @GetMapping("/{id}/readiness")
    public ReadinessDto readiness(@PathVariable Long id, @CurrentUser UserPrincipal principal) {
        return targetService.getReadiness(id, principal.id(), principal.admin());
    }
}
