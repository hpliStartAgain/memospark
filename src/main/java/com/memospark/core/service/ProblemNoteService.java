package com.memospark.core.service;

import com.memospark.core.domain.*;
import com.memospark.core.dto.*;
import com.memospark.core.repository.CodeProblemRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.SrsSettingsRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemNoteService {

    private final ProblemNoteRepository noteRepository;
    private final CodeProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final SrsEngine srsEngine;
    private final SrsSettingsRepository srsSettingsRepository;

    @Transactional(readOnly = true)
    public List<ProblemNoteDto> getAllNotes(Long userId) {
        return noteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProblemNoteDto> getNotesByType(Long userId, String filter) {
        if ("STARRED".equals(filter)) {
            return noteRepository.findByUserIdAndStarredTrueOrderByUpdatedAtDesc(userId)
                    .stream().map(this::toDto).toList();
        }
        BookmarkType type = BookmarkType.valueOf(filter);
        return noteRepository.findByUserIdAndBookmarkTypeOrderByUpdatedAtDesc(userId, type)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ProblemNoteDto> getDueNotes(Long userId) {
        return noteRepository.findByUserIdAndNextRetryDateLessThanEqual(userId, LocalDate.now())
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(Long userId) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("wrong", noteRepository.countByUserIdAndBookmarkType(userId, BookmarkType.WRONG));
        summary.put("starred", noteRepository.countByUserIdAndStarredTrue(userId));
        summary.put("todo", noteRepository.countByUserIdAndBookmarkType(userId, BookmarkType.TODO));
        summary.put("dueRetries", noteRepository.findByUserIdAndNextRetryDateLessThanEqual(userId, LocalDate.now()).size());
        return summary;
    }

    /** Toggle star — independent of bookmarkType. Creates note if needed. */
    @Transactional
    public Map<String, Object> toggleStar(Long userId, Long problemId) {
        ProblemNote note = getOrCreate(userId, problemId);
        note.setStarred(!note.isStarred());
        noteRepository.save(note);
        cleanupIfEmpty(note);
        return Map.of("starred", note.isStarred());
    }

    /** Toggle TODO — independent of starred. Creates note if needed. */
    @Transactional
    public Map<String, Object> toggleTodo(Long userId, Long problemId) {
        ProblemNote note = getOrCreate(userId, problemId);
        if (note.getBookmarkType() == BookmarkType.TODO) {
            note.setBookmarkType(null); // un-toggle
        } else {
            note.setBookmarkType(BookmarkType.TODO);
        }
        noteRepository.save(note);
        cleanupIfEmpty(note);
        boolean isTodo = note.getBookmarkType() == BookmarkType.TODO;
        return Map.of("todo", isTodo);
    }

    /** Save as WRONG note with error reason + text. Creates or updates. */
    @Transactional
    public ProblemNoteDto saveWrongNote(Long userId, Long problemId, String noteText, String errorReason) {
        ProblemNote note = getOrCreate(userId, problemId);
        note.setBookmarkType(BookmarkType.WRONG);
        note.setNote(noteText);
        if (errorReason != null && !errorReason.isBlank()) {
            note.setErrorReason(ErrorReason.valueOf(errorReason));
        } else {
            note.setErrorReason(null);
        }
        // Set initial retry schedule if none
        if (note.getNextRetryDate() == null) {
            note.setRetryInterval(1);
            note.setNextRetryDate(LocalDate.now().plusDays(1));
        }
        return toDto(noteRepository.save(note));
    }

    /** Remove WRONG status (keep starred if set). */
    @Transactional
    public void removeWrongNote(Long userId, Long problemId) {
        noteRepository.findByUserIdAndProblemId(userId, problemId).ifPresent(note -> {
            note.setBookmarkType(null);
            note.setErrorReason(null);
            note.setNote(null);
            note.setRetryCount(0);
            note.setRetryInterval(0);
            note.setNextRetryDate(null);
            note.setLastRetryDate(null);
            noteRepository.save(note);
            cleanupIfEmpty(note);
        });
    }

    @Transactional
    public ProblemNoteDto recordRetry(Long userId, Long problemId, int quality) {
        ProblemNote note = noteRepository.findByUserIdAndProblemId(userId, problemId)
                .orElseThrow(() -> new NoSuchElementException("Note not found"));

        note.setRetryCount(note.getRetryCount() + 1);
        note.setLastRetryDate(LocalDate.now());

        // Read user's SRS settings instead of hardcoded defaults
        SrsSettings settings = srsSettingsRepository.findByUserId(userId).orElse(null);
        double minEase = settings != null ? settings.getMinEaseFactor() : SrsEngine.DEFAULT_MIN_EASE_FACTOR;
        int firstInterval = settings != null ? settings.getFirstInterval() : 1;
        int secondInterval = settings != null ? settings.getSecondInterval() : 6;

        double ef = srsEngine.nextEaseFactor(
                note.getEaseFactor(), quality, minEase);
        note.setEaseFactor(ef);

        if (quality >= 3) {
            // repetitions is 0-based: retryCount was just incremented, so subtract 1.
            note.setRetryInterval(srsEngine.nextIntervalOnSuccess(
                    note.getRetryCount() - 1, note.getRetryInterval(), ef, firstInterval, secondInterval));
        } else {
            note.setRetryInterval(firstInterval);
        }

        note.setNextRetryDate(LocalDate.now().plusDays(note.getRetryInterval()));
        return toDto(noteRepository.save(note));
    }

    @Transactional(readOnly = true)
    public WeaknessAnalysisDto getWeaknessData(Long userId) {
        Map<String, Integer> errorCounts = new LinkedHashMap<>();
        for (Object[] row : noteRepository.countWrongByErrorReason(userId)) {
            errorCounts.put(((ErrorReason) row[0]).name(), ((Long) row[1]).intValue());
        }
        Map<String, Integer> catCounts = new LinkedHashMap<>();
        for (Object[] row : noteRepository.countWrongByCategory(userId)) {
            String cat = row[0] != null ? (String) row[0] : "Other";
            catCounts.put(cat, ((Long) row[1]).intValue());
        }
        int totalWrong = noteRepository.countByUserIdAndBookmarkType(userId, BookmarkType.WRONG);
        return new WeaknessAnalysisDto(errorCounts, catCounts, totalWrong, null);
    }

    /** Get or create a note record for the user+problem. */
    private ProblemNote getOrCreate(Long userId, Long problemId) {
        return noteRepository.findByUserIdAndProblemId(userId, problemId).orElseGet(() -> {
            ProblemNote n = new ProblemNote();
            n.setUser(userRepository.findById(userId).orElseThrow());
            n.setProblem(problemRepository.findById(problemId).orElseThrow());
            return noteRepository.save(n);
        });
    }

    /** Delete the note row if it has no meaningful data (not starred, no type). */
    private void cleanupIfEmpty(ProblemNote note) {
        if (!note.isStarred() && note.getBookmarkType() == null && (note.getNote() == null || note.getNote().isBlank())) {
            noteRepository.delete(note);
        }
    }

    private ProblemNoteDto toDto(ProblemNote n) {
        CodeProblem p = n.getProblem();
        boolean isDue = n.getNextRetryDate() != null && !n.getNextRetryDate().isAfter(LocalDate.now());
        return new ProblemNoteDto(
                n.getId(), p.getId(), p.getProblemNumber(), p.getTitle(),
                p.getDifficulty(), p.getCategory(),
                n.getBookmarkType() != null ? n.getBookmarkType().name() : null,
                n.isStarred(), n.getNote(),
                n.getErrorReason() != null ? n.getErrorReason().name() : null,
                n.getRetryCount(), n.getNextRetryDate(), isDue, n.getUpdatedAt()
        );
    }
}
