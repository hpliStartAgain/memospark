package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.*;
import com.memospark.core.service.MockInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/targets/{targetId}/mock-interviews")
@RequiredArgsConstructor
public class MockInterviewController {

    private final MockInterviewService mockInterviewService;

    @GetMapping
    public List<MockInterviewDto> list(@PathVariable Long targetId,
                                       @CurrentUser UserPrincipal principal) {
        return mockInterviewService.list(targetId, principal.id(), principal.admin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MockInterviewDto start(@PathVariable Long targetId,
                                  @RequestBody(required = false) StartMockInterviewRequest req,
                                  @CurrentUser UserPrincipal principal) {
        return mockInterviewService.start(targetId, principal.id(), principal.admin(), req);
    }

    @GetMapping("/{interviewId}")
    public MockInterviewDto get(@PathVariable Long targetId,
                                @PathVariable Long interviewId,
                                @CurrentUser UserPrincipal principal) {
        return mockInterviewService.get(targetId, interviewId, principal.id(), principal.admin());
    }

    @PostMapping("/{interviewId}/questions/{questionId}/answer")
    public MockInterviewDto answer(@PathVariable Long targetId,
                                   @PathVariable Long interviewId,
                                   @PathVariable Long questionId,
                                   @RequestBody AnswerMockInterviewRequest req,
                                   @CurrentUser UserPrincipal principal) {
        return mockInterviewService.answer(targetId, interviewId, questionId, principal.id(), principal.admin(), req);
    }

    @PostMapping("/{interviewId}/finish")
    public MockInterviewDto finish(@PathVariable Long targetId,
                                   @PathVariable Long interviewId,
                                   @CurrentUser UserPrincipal principal) {
        return mockInterviewService.finish(targetId, interviewId, principal.id(), principal.admin());
    }
}
