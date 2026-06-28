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
 * Flashcard scheduling service.
 *
 * <p>Scheduling uses FSRS stability/difficulty/retention. The legacy SM-2 ease
 * factor is still updated as a compatibility signal for existing UI and data.</p>
 */
@Service
@RequiredArgsConstructor
public class SpacedRepetitionService {

    private final SrsSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final FsrsEngine fsrsEngine;
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
        FsrsEngine.FsrsResult fsrs = fsrsEngine.next(
                new FsrsEngine.CardMemory(
                        progress.getStability(),
                        progress.getDifficulty(),
                        progress.getLastReviewDate()),
                quality,
                LocalDate.now(),
                normalizedDesiredRetention(settings.getDesiredRetention()),
                settings.getFirstInterval());

        if (quality >= 3) {
            repetitions++;
        } else {
            repetitions = 0;
        }

        progress.setRepetitions(repetitions);
        progress.setEaseFactor(easeFactor);
        progress.setStability(fsrs.stability());
        progress.setDifficulty(fsrs.difficulty());
        progress.setInterval(fsrs.interval());
        progress.setNextReviewDate(LocalDate.now().plusDays(fsrs.interval()));
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
        progress.setStability(FsrsEngine.DEFAULT_STABILITY);
        progress.setDifficulty(FsrsEngine.DEFAULT_DIFFICULTY);
        progress.setInterval(0);
        progress.setNextReviewDate(LocalDate.now());
        progress.setLastReviewDate(null);
    }

    public SrsSettingsDto getSrsSettings(Long userId) {
        SrsSettings s = getSettings(userId);
        return new SrsSettingsDto(
                s.getInitialEaseFactor(),
                s.getMinEaseFactor(),
                s.getFirstInterval(),
                s.getSecondInterval(),
                s.getDesiredRetention());
    }

    @Transactional
    public SrsSettingsDto updateSrsSettings(Long userId, SrsSettingsDto dto) {
        SrsSettings s = getSettings(userId);
        s.setInitialEaseFactor(dto.initialEaseFactor());
        s.setMinEaseFactor(dto.minEaseFactor());
        s.setFirstInterval(dto.firstInterval());
        s.setSecondInterval(dto.secondInterval());
        s.setDesiredRetention(normalizedDesiredRetention(dto.desiredRetention()));
        settingsRepository.save(s);
        return getSrsSettings(userId);
    }

    private double normalizedDesiredRetention(double value) {
        if (value <= 0.0) {
            return FsrsEngine.DEFAULT_DESIRED_RETENTION;
        }
        return Math.max(0.7, Math.min(0.98, value));
    }
}
