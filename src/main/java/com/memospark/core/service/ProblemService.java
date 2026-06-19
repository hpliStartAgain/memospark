package com.memospark.core.service;

import com.memospark.core.domain.CodeProblem;
import com.memospark.core.domain.CodeSubmission;
import com.memospark.core.domain.ProblemNote;
import com.memospark.core.dto.*;
import com.memospark.core.repository.CodeProblemRepository;
import com.memospark.core.repository.CodeSubmissionRepository;
import com.memospark.core.repository.ProblemNoteRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemService {

    private final CodeProblemRepository problemRepository;
    private final CodeSubmissionRepository submissionRepository;
    private final ProblemNoteRepository noteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProblemSummaryDto> getAllProblemsSummary(Long userId) {
        // All batch-fetched — zero N+1 per problem
        Map<Long, ProblemNote> noteMap = new HashMap<>();
        Map<Long, Integer> failMap = new HashMap<>();
        Set<Long> acceptedIds = new HashSet<>();
        Map<Long, Integer> attemptMap = new HashMap<>();

        if (userId != null) {
            noteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                    .forEach(n -> noteMap.put(n.getProblem().getId(), n));
            for (Object[] row : submissionRepository.countFailsByUser(userId)) {
                failMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            for (Object[] row : submissionRepository.countAttemptsByUser(userId)) {
                attemptMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            submissionRepository.findAcceptedProblemIdsByUser(userId)
                    .forEach(acceptedIds::add);
        }

        return problemRepository.findAllByOrderByProblemNumberAsc()
                .stream().map(p -> {
                    ProblemNote note = noteMap.get(p.getId());
                    String bm = note != null && note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                    boolean starred = note != null && note.isStarred();
                    int failCount = failMap.getOrDefault(p.getId(), 0);
                    int attemptCount = attemptMap.getOrDefault(p.getId(), 0);
                    boolean accepted = acceptedIds.contains(p.getId());
                    return new ProblemSummaryDto(
                            p.getId(), p.getProblemNumber(), p.getTitle(), p.getDifficulty(),
                            p.getCategory(), p.getTags(), p.getHint(),
                            accepted, bm, starred, failCount, attemptCount);
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<CodeProblemDto> getAllProblems(Long userId) {
        Map<Long, ProblemNote> noteMap = new HashMap<>();
        Map<Long, Integer> failMap = new HashMap<>();
        Set<Long> acceptedIds = new HashSet<>();
        Map<Long, Integer> attemptMap = new HashMap<>();

        if (userId != null) {
            noteRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                    .forEach(n -> noteMap.put(n.getProblem().getId(), n));
            for (Object[] row : submissionRepository.countFailsByUser(userId)) {
                failMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            for (Object[] row : submissionRepository.countAttemptsByUser(userId)) {
                attemptMap.put((Long) row[0], ((Long) row[1]).intValue());
            }
            submissionRepository.findAcceptedProblemIdsByUser(userId)
                    .forEach(acceptedIds::add);
        }
        final Map<Long, ProblemNote> notes = noteMap;
        final Map<Long, Integer> fails = failMap;
        final Set<Long> accepted = acceptedIds;
        final Map<Long, Integer> attempts = attemptMap;
        return problemRepository.findAllByOrderByProblemNumberAsc()
                .stream().map(p -> {
                    ProblemNote note = notes.get(p.getId());
                    String bm = note != null && note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                    boolean starred = note != null && note.isStarred();
                    int failCount = fails.getOrDefault(p.getId(), 0);
                    int attemptCount = attempts.getOrDefault(p.getId(), 0);
                    return toDtoWithCounts(p, accepted.contains(p.getId()), bm, starred, failCount, attemptCount);
                }).toList();
    }

    @Transactional(readOnly = true)
    public CodeProblemDetailDto getProblem(Long id, Long userId) {
        CodeProblem p = getProblemOrThrow(id);
        String bookmark = null;
        boolean starred = false;
        int failCount = 0;
        if (userId != null) {
            var noteOpt = noteRepository.findByUserIdAndProblemId(userId, id);
            if (noteOpt.isPresent()) {
                ProblemNote note = noteOpt.get();
                bookmark = note.getBookmarkType() != null ? note.getBookmarkType().name() : null;
                starred = note.isStarred();
            }
            failCount = submissionRepository.countByProblemIdAndUserId(id, userId);
        }
        boolean accepted = userId != null && submissionRepository.existsByProblemIdAndUserIdAndStatus(id, userId, "ACCEPTED");
        int attemptCount = userId != null ? submissionRepository.countByProblemIdAndUserId(id, userId) : 0;
        return toDetailDto(p, accepted, bookmark, starred, failCount, attemptCount);
    }

    @Transactional(readOnly = true)
    public List<CodeSubmissionDto> getSubmissions(Long problemId, Long userId) {
        return submissionRepository.findByProblemIdAndUserIdOrderBySubmittedAtDesc(problemId, userId)
                .stream()
                .map(s -> new CodeSubmissionDto(
                        s.getId(), s.getLanguage(), s.getStatus(),
                        s.getPassedCases(), s.getTotalCases(), s.getSubmittedAt()))
                .toList();
    }

    private CodeProblem getProblemOrThrow(Long id) {
        return problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found: " + id));
    }

    @Transactional
    public CodeProblemDto createProblem(CreateProblemRequest req) {
        CodeProblem p = new CodeProblem(
                req.problemNumber(), req.title(), req.difficulty(),
                req.description(), req.javaTemplate(), req.pythonTemplate(),
                req.javaDriverCode(), req.pythonDriverCode(),
                req.testCasesJson(), req.tags());
        p.setHint(req.hint());
        p.setCategory(req.category());
        p = problemRepository.save(p);
        return toDtoWithCounts(p, false, null, false, 0, 0);
    }

    @Transactional
    public CodeProblemDto updateProblem(Long id, CreateProblemRequest req) {
        CodeProblem p = getProblemOrThrow(id);
        if (req.problemNumber() != null) p.setProblemNumber(req.problemNumber());
        if (req.title() != null) p.setTitle(req.title());
        if (req.difficulty() != null) p.setDifficulty(req.difficulty());
        if (req.description() != null) p.setDescription(req.description());
        if (req.hint() != null) p.setHint(req.hint());
        if (req.javaTemplate() != null) p.setJavaTemplate(req.javaTemplate());
        if (req.pythonTemplate() != null) p.setPythonTemplate(req.pythonTemplate());
        if (req.javaDriverCode() != null) p.setJavaDriverCode(req.javaDriverCode());
        if (req.pythonDriverCode() != null) p.setPythonDriverCode(req.pythonDriverCode());
        if (req.testCasesJson() != null) p.setTestCasesJson(req.testCasesJson());
        if (req.tags() != null) p.setTags(req.tags());
        if (req.category() != null) p.setCategory(req.category());
        p = problemRepository.save(p);
        return toDtoWithCounts(p, false, null, false, 0, 0);
    }

    @Transactional
    public void deleteProblem(Long id) {
        // Detach notes so users keep their review notes
        noteRepository.clearProblemReference(id);
        submissionRepository.deleteByProblemId(id);
        problemRepository.deleteById(id);
    }

    private CodeProblemDto toDtoWithCounts(CodeProblem p, boolean accepted, String bookmarkType,
                                            boolean starred, int failCount, int attemptCount) {
        return new CodeProblemDto(
                p.getId(), p.getProblemNumber(), p.getTitle(), p.getDifficulty(),
                p.getDescription(), p.getHint(),
                p.getJavaTemplate(), p.getPythonTemplate(),
                p.getJavaDriverCode(), p.getPythonDriverCode(),
                p.getTestCasesJson(), p.getTags(), p.getCategory(),
                accepted, bookmarkType, starred, failCount, attemptCount);
    }

    private CodeProblemDetailDto toDetailDto(CodeProblem p, boolean accepted, String bookmarkType,
                                              boolean starred, int failCount, int attemptCount) {
        return new CodeProblemDetailDto(
                p.getId(), p.getProblemNumber(), p.getTitle(), p.getDifficulty(),
                p.getDescription(), p.getHint(),
                p.getJavaTemplate(), p.getPythonTemplate(),
                p.getTags(), p.getCategory(),
                accepted, bookmarkType, starred, failCount, attemptCount);
    }

}
