package com.memospark.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * Local Docker-based code execution backend.
 * Active when judge.type=container.
 * <p>
 * Runs code inside a Docker container with CPU/memory limits and captures stdout/stderr.
 * Uses the same Judge0 status ID convention for compatibility with JudgeOrchestrator.
 */
@Service
@ConditionalOnProperty(name = "judge.type", havingValue = "container")
@Slf4j
public class ContainerJudgeService implements JudgeBackend {

    @Value("${judge.container.image.java:openjdk:17-slim}")
    private String javaImage;

    @Value("${judge.container.image.python:python:3.11-slim}")
    private String pythonImage;

    @Value("${judge.container.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${judge.container.memory-mb:256}")
    private int memoryMb;

    @Override
    public JudgeResult execute(String sourceCode, String language, String stdin) {
        String image = "java".equals(language) ? javaImage : pythonImage;
        String fileName = "java".equals(language) ? "Main.java" : "solution.py";
        String workDir = "/app";

        Path hostTempDir;
        try {
            hostTempDir = Files.createTempDirectory("judge_");
            Path codeFile = hostTempDir.resolve(fileName);
            Files.writeString(codeFile, sourceCode, StandardCharsets.UTF_8);
            Path inputFile = hostTempDir.resolve("input.txt");
            Files.writeString(inputFile, stdin != null ? stdin : "", StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to prepare temp files for container judge", e);
            return new JudgeResult(13, "", "Failed to prepare execution: " + e.getMessage(), "");
        }

        try {
            String containerName = "judge_" + UUID.randomUUID().toString().substring(0, 8);

            // Build the run command
            String runCmd = buildRunCommand(language, fileName, workDir);

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "--name", containerName,
                    "--memory", memoryMb + "m",
                    "--cpus", "1.0",
                    "--network", "none",
                    "--read-only",
                    "-v", hostTempDir.toAbsolutePath() + ":" + workDir + ":ro",
                    "-w", workDir,
                    image,
                    "sh", "-c", runCmd
            );
            pb.redirectErrorStream(false);

            Process process = pb.start();

            // Write stdin to process input
            if (stdin != null && !stdin.isEmpty()) {
                process.getOutputStream().write(stdin.getBytes(StandardCharsets.UTF_8));
                process.getOutputStream().close();
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                killContainer(containerName);
                return new JudgeResult(5, "", "Time Limit Exceeded", "");
            }

            String stdout = readAll(process.getInputStream());
            String stderr = readAll(process.getErrorStream());
            int exitCode = process.exitValue();

            // Compile error detection
            if (exitCode != 0 && stderr.contains("error:") && "java".equals(language)) {
                return new JudgeResult(6, "", stderr, stderr);
            }
            if (exitCode != 0 && stderr.contains("SyntaxError") && "python".equals(language)) {
                return new JudgeResult(6, "", stderr, stderr);
            }

            // Runtime error
            if (exitCode != 0) {
                return new JudgeResult(7, stdout, stderr.isEmpty() ? "Runtime Error" : stderr, "");
            }

            // Success — Judge0 status 3
            return new JudgeResult(3, stdout, "", "");

        } catch (Exception e) {
            log.error("Container judge execution failed", e);
            return new JudgeResult(13, "", "Execution failed: " + e.getMessage(), "");
        } finally {
            // Cleanup temp files
            try {
                Files.list(hostTempDir).forEach(f -> {
                    try { Files.deleteIfExists(f); } catch (Exception ignored) {}
                });
                Files.deleteIfExists(hostTempDir);
            } catch (Exception ignored) {}
        }
    }

    private String buildRunCommand(String language, String fileName, String workDir) {
        if ("java".equals(language)) {
            return "javac " + fileName + " 2> compile_err.txt && java -cp " + workDir + " Main < input.txt 2> run_err.txt; cat compile_err.txt";
        } else {
            return "python3 " + fileName + " < input.txt 2> run_err.txt";
        }
    }

    private String readAll(java.io.InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void killContainer(String name) {
        try {
            new ProcessBuilder("docker", "kill", name).start().waitFor(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
    }
}
