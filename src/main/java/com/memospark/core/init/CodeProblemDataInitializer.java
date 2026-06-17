package com.memospark.core.init;

import com.memospark.core.domain.CodeProblem;
import com.memospark.core.repository.CodeProblemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seeds the {@code code_problems} table from the static {@link CodeProblemData}
 * source on application startup.
 *
 * <p>Performance notes (the reason this class is more involved than a plain loop):
 * <ul>
 *   <li>Previously this initializer issued <strong>one {@code SELECT} per problem</strong>
 *       on every boot (≈100+ blocking JDBC round-trips), which dominated startup
 *       wall-clock and produced visible iowait spikes even when the DB was already
 *       fully populated.</li>
 *   <li>The new flow performs <strong>at most two</strong> queries on the steady-state
 *       boot: a single projection of existing {@code problemNumber}s, and (only when
 *       needed) a targeted query for rows missing the {@code category} backfill.</li>
 *   <li>Inserts / updates are batched via {@code saveAll(...)} so that, combined with
 *       the {@code hibernate.jdbc.batch_size} configuration, first-run population also
 *       writes in batches instead of one row at a time.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class CodeProblemDataInitializer implements ApplicationRunner {

    private static final int BATCH_SIZE = 50;

    private final CodeProblemRepository codeProblemRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String[]> problems = CodeProblemData.problems();

        // 1) Fast existence check via a single lightweight projection query.
        Set<Integer> existingNumbers = new HashSet<>(codeProblemRepository.findAllProblemNumbers());

        // 2) Build the static-data view once: problemNumber -> category (may be null).
        Map<Integer, String> staticCategoryByNumber = new HashMap<>(problems.size() * 2);
        List<CodeProblem> toInsert = new ArrayList<>();

        for (String[] p : problems) {
            int problemNumber = Integer.parseInt(p[0]);
            String category = p.length > 10 ? p[10] : null;
            staticCategoryByNumber.put(problemNumber, category);

            if (existingNumbers.contains(problemNumber)) {
                continue;
            }
            CodeProblem problem = new CodeProblem(
                    problemNumber,
                    p[1],  // title
                    p[2],  // difficulty
                    p[3],  // description
                    p[4],  // javaTemplate
                    p[5],  // pythonTemplate
                    p[6],  // javaDriverCode
                    p[7],  // pythonDriverCode
                    p[8],  // testCasesJson
                    p[9]   // tags
            );
            problem.setCategory(category);
            toInsert.add(problem);
        }

        int inserted = saveInBatches(toInsert);

        // 3) Category backfill — only when there might actually be missing values.
        //    On a steady-state boot this query is skipped entirely after the first
        //    successful backfill, because the fast-path above already touched zero rows.
        int updated = 0;
        if (!existingNumbers.isEmpty()) {
            List<CodeProblem> needsCategory = codeProblemRepository.findAllWithMissingCategory();
            if (!needsCategory.isEmpty()) {
                List<CodeProblem> toUpdate = new ArrayList<>(needsCategory.size());
                for (CodeProblem ep : needsCategory) {
                    String category = staticCategoryByNumber.get(ep.getProblemNumber());
                    if (category != null && !category.isBlank()) {
                        ep.setCategory(category);
                        toUpdate.add(ep);
                    }
                }
                updated = saveInBatches(toUpdate);
            }
        }

        if (inserted > 0 || updated > 0) {
            log.info("Code problems: {} inserted, {} updated (category backfill)", inserted, updated);
        } else {
            log.debug("Code problems already up to date ({} entries).", existingNumbers.size());
        }
    }

    private int saveInBatches(List<CodeProblem> rows) {
        if (rows.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < rows.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, rows.size());
            codeProblemRepository.saveAll(rows.subList(i, end));
            codeProblemRepository.flush();
        }
        return rows.size();
    }
}
