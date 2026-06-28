# Verification

| Area | Command | Result | Evidence |
| --- | --- | --- | --- |
| Backend tests | `.\mvnw.cmd test` | Passed | `.agent/verify-mvn-test.log` |
| Web frontend | `npm run build` from `frontend/` | Passed | `.agent/verify-frontend-build.log` |
| MCP server | `npm run build` from `mcp-server/` | Passed | `.agent/verify-mcp-build.log` |
| WeChat mini-program | `npm run build:weapp` from `miniprogram/` | Passed | `.agent/verify-miniprogram-build.log` |
| Maven package | `.\mvnw.cmd -DskipTests package` | Passed on retry | `.agent/verify-mvn-package-2.log` |
| Docker image | `docker build -t memospark:codex-verify .` | Blocked locally | `.agent/verify-docker-build.log` |
| CI/CD unit test check | `.\mvnw.cmd -B test` | Passed | terminal output, 42 tests |
| GitHub Actions secrets | GitHub REST API repository secrets update | Passed | `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY` set |
| Remote Docker build | `docker compose -f docker-compose.prod.yml build app` on `117.72.99.55` | Passed | `/tmp/memospark-build2.log` |
| Remote deploy | `docker compose -f docker-compose.prod.yml up -d app` on `117.72.99.55` | Passed | `memospark-app` healthy |
| Remote health | `curl http://127.0.0.1:8080/actuator/health` on host | Passed | `{"status":"UP"}` |
| Public page | `Invoke-WebRequest http://117.72.99.55:8080/` from local machine | Passed | HTTP 200, title `MemoSpark — 面试记忆引擎` |
| Flyway migrations | Query `flyway_schema_history` and target tables on host MySQL | Passed | V1-V5 successful; `targets`, `job_jds`, `target_skills` present |

Additional notes:

- The first Maven package attempt failed with a Windows `EBUSY` file-lock while Vite was deleting a generated asset; the retry passed without code changes.
- Docker CLI is installed, but `com.docker.service` is stopped and the daemon pipe is unavailable.
- Web build reports a large chunk warning only; it does not fail the build.
- The in-app Browser plugin timed out while navigating to the public IP, but local `Invoke-WebRequest` and host-side curl both verified the service.
