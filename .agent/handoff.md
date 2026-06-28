# Handoff

Ready state:

- Backend tests pass.
- Web, MCP, and mini-program builds pass.
- Maven package produces `target/memospark-0.0.1-SNAPSHOT.jar`.
- Runtime and documentation configuration are aligned around the documented environment variable names.

CI/CD deployment state:

- GitHub Actions workflow now includes deploy after tests and Docker build validation.
- Repository secrets are configured: `DEPLOY_HOST`, `DEPLOY_PORT`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`.
- Server production directory is `/opt/memospark`.
- Server production env file is `/opt/memospark/.env` and is intentionally not in git.
- Current production container is `memospark-app`, exposed at `117.72.99.55:8080`, and healthy.
- Flyway history on production MySQL includes V1-V5; target interview tables exist.

Remaining local-environment action:

- Local Docker Desktop / `com.docker.service` was previously unavailable; remote Docker build passed. If local Docker validation is still desired, start Docker Desktop and rerun:

```powershell
docker build -t memospark:codex-verify .
docker compose up --build
```

Review before commit:

- The worktree contains pre-existing frontend and mini-program edits. Review them together with the fixes from this pass before staging.
- `replace.js` and `replace.py` are untracked pre-existing helper files and were left untouched.
- Commit and push are still required for GitHub Actions to take over future deployments from the repository.
