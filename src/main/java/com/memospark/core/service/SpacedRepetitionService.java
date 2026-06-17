package com.memospark.core.service;

import com.memospark.core.domain.CardProgress;
import com.memospark.core.domain.SrsSettings;
import com.memospark.core.domain.User;
import com.memospark.core.dto.SrsSettingsDto;
import com.memospark.core.repository.SrsSettingsRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * SM-2 algorithm with configurable per-user parameters.
 */
@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private final SrsSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final SrsEngine srsEngine;

    private SrsSettings getSettings(Long userId) {
        return settingsRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId).orElse(null);
            SrsSettings s = new SrsSettings(user);
            return settingsRepository.save(s);
        });
    }

    public CardProgress applyReview(CardProgress progress, int quality, Long userId) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }

        SrsSettings settings = getSettings(userId);

        int repetitions = progress.getRepetitions();
        double easeFactor = srsEngine.nextEaseFactor(
                progress.getEaseFactor(), quality, settings.getMinEaseFactor());
        int interval;

        if (quality >= 3) {
            interval = srsEngine.nextIntervalOnSuccess(
                    repetitions, progress.getInterval(), easeFactor,
                    settings.getFirstInterval(), settings.getSecondInterval());
            repetitions++;
        } else {
            repetitions = 0;
            interval = settings.getFirstInterval();
        }

        progress.setRepetitions(repetitions);
        progress.setEaseFactor(easeFactor);
        progress.setInterval(interval);
        progress.setNextReviewDate(LocalDate.now().plusDays(interval));
        if (progress.getLastReviewDate() == null) {
            progress.setFirstLearnedDate(LocalDate.now());
        }
        progress.setLastReviewDate(LocalDate.now());

        return progress;
    }

    public void initProgress(CardProgress progress, Long userId) {
        SrsSettings settings = getSettings(userId);
        progress.setRepetitions(0);
        progress.setEaseFactor(settings.getInitialEaseFactor());
        progress.setInterval(0);
        progress.setNextReviewDate(LocalDate.now());
        progress.setLastReviewDate(null);
    }

    public SrsSettingsDto getSrsSettings(Long userId) {
        SrsSettings s = getSettings(userId);
        return new SrsSettingsDto(s.getInitialEaseFactor(), s.getMinEaseFactor(), s.getFirstInterval(), s.getSecondInterval());
    }

    @Transactional
    public SrsSettingsDto updateSrsSettings(Long userId, SrsSettingsDto dto) {
        SrsSettings s = getSettings(userId);
        s.setInitialEaseFactor(dto.initialEaseFactor());
        s.setMinEaseFactor(dto.minEaseFactor());
        s.setFirstInterval(dto.firstInterval());
        s.setSecondInterval(dto.secondInterval());
        settingsRepository.save(s);
        return dto;
    }
}
