package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.DailyStatsDto;
import com.memospark.core.dto.StatsDto;
import com.memospark.core.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public StatsDto getStats(@CurrentUser UserPrincipal principal) {
        return statisticsService.getStats(principal.id());
    }

    @GetMapping("/daily")
    public List<DailyStatsDto> getDailyStats(@RequestParam(defaultValue = "30") int days,
                                              @CurrentUser UserPrincipal principal) {
        return statisticsService.getDailyStats(principal.id(), Math.min(days, 90));
    }
}
