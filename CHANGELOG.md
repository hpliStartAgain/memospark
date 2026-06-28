# Changelog

## 2026-06-28

- Added GitHub Actions deployment for push/manual CI/CD, using SSH to run `scripts/deploy.sh` on the production host after tests and Docker build validation pass.
- Added `docker-compose.prod.yml` and `.env.production.example` for an app-only production container that connects to the host MySQL instance.
- Added `scripts/deploy.sh` to update `/opt/memospark`, build the Docker image, start the app, and verify actuator health.
- Added a dedicated GitHub Actions deploy SSH key and repository secrets for the production host.
- Fixed Spring Boot 4 Flyway startup integration by using `spring-boot-starter-flyway`, allowing V4/V5 migrations to run before JPA schema validation.
- Optimized Docker builds with BuildKit cache mounts for Maven, npm, and the frontend Maven plugin's local Node install.
- Deployed MemoSpark to `117.72.99.55:8080` with Docker Compose; Flyway applied V4/V5 and the production container is healthy.
- Fixed backend test bootstrap for the Spring Boot 4 / Java 21 stack by adding test-only H2 and Spring Security test support.
- Added a test profile under `src/test/resources/application.properties` so local tests use in-memory H2 instead of requiring MySQL or external AI/Judge services.
- Updated controller and service tests for the current security and DTO contracts.
- Hardened AI grading fallback behavior so temporary AI API failures return a safe default grade instead of failing the review flow.
- Fixed MCP deck summaries to read the backend `dueCards` field while keeping compatibility with `due`.
- Locked the Taro webpack 5 build chain to webpack `5.78.0` via npm overrides to avoid the webpackbar / ProgressPlugin incompatibility.
- Aligned Docker build/runtime images with the project's Java 21 baseline and included the Web frontend source in the Docker build context.
- Aligned Docker Compose, `.env.example`, `README.md`, and application configuration around the same environment variable names.
- Removed duplicate production configuration blocks from `src/main/resources/application.properties`.
- Added `.dockerignore` and ignored local generated logs/build folders.
- Rebuilt the Web static assets under `src/main/resources/static`.

### Verification

- Passed: `.\mvnw.cmd test`
- Passed: `npm run build` in `frontend/`
- Passed: `npm run build` in `mcp-server/`
- Passed: `npm run build:weapp` in `miniprogram/`
- Passed: `.\mvnw.cmd -DskipTests package`
- Passed: production Docker image build on `117.72.99.55`
- Passed: production `docker compose -f docker-compose.prod.yml up -d` and `GET /actuator/health`
- Passed: public `GET http://117.72.99.55:8080/` returns `200` with title `MemoSpark — 面试记忆引擎`
- Blocked by local environment: Docker image build could not connect to the Docker daemon because `com.docker.service` was stopped.
