package com.memospark.core.service;

/**
 * Abstraction for code execution backends.
 * Implementations: Judge0 cloud ({@link Judge0JudgeService}) and local Docker ({@link ContainerJudgeService}).
 */
public interface JudgeBackend {

    /**
     * Execute source code with given stdin and return the result.
     *
     * Judge0 status IDs:
     * 3 = Accepted (code ran successfully — output comparison done by caller)
     * 5 = Time Limit Exceeded
     * 6 = Compilation Error
     * 7-12 = Various Runtime Errors
     * 13 = Internal Error
     */
    JudgeResult execute(String sourceCode, String language, String stdin);

    record JudgeResult(int statusId, String stdout, String stderr, String compileOutput) {}
}
