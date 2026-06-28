# MemoSpark 全量交付验证 TODO

## 需求理解

- 全面测试当前项目，包括后端 Spring Boot、Web 前端、微信小程序、MCP Server。
- 修复发现的代码、配置、构建、测试和文档一致性问题。
- 保持现有未提交改动，不回滚用户已有工作。
- 交付时给出验证命令、结果、剩余风险和是否需要提交/推送。

## 事实源

- 根目录 `README.md`
- 根目录 `pom.xml` / `Makefile`
- `frontend/package.json`
- `miniprogram/package.json`
- `mcp-server/package.json` / `mcp-server/README.md`
- 当前 git 状态

## 阶段计划

1. [done] 读取项目规范、README、构建脚本、子项目配置。
2. [done] 跑后端测试与 Maven verify/package，记录失败。
3. [done] 跑 Web 前端 TypeScript/Vite build，记录失败。
4. [done] 跑 MCP Server TypeScript build，记录失败。
5. [done] 跑微信小程序构建，记录失败。
6. [done] 修复代码、配置与文档不一致。
7. [done] 复跑关键验证，更新 `CHANGELOG.md` 和 `.agent/` 交付记录。

## 外部变更

- 不新增外部服务写操作。
- 不自动 commit、push、发布。
- Notion 同步因本机 `ntn` CLI 不存在，跳过远端任务更新。

## 待确认问题

- Docker CLI 已安装，但本机 Docker daemon 未运行；镜像构建验证已记录为本地环境阻塞。

## 2026-06-28 CI/CD 部署任务

### 需求理解

- push 到 GitHub 后自动触发构建，并部署到 `117.72.99.55`。
- 生产服务跑在 SSH 主机上，数据库复用同一主机的 MySQL `53306`。
- 不把 SSH 密码、数据库密码、AI key、JWT secret 写进仓库。

### 阶段计划

1. [done] 读取现有 README、Dockerfile、compose、CI workflow 和远端运行状态。
2. [done] 验证 SSH key、Docker、Compose、MySQL 端口和当前服务健康状态。
3. [done] 补齐生产 compose、部署脚本、GitHub Actions deploy job 和环境模板。
4. [done] 自动配置 GitHub Actions Secrets 和服务器 `/opt/memospark/.env`。
5. [done] 停止远端开发态 Maven 进程，切换为 Docker Compose 生产态。
6. [done] 修复 Spring Boot 4 Flyway auto-configuration 依赖缺失，重建并验证 `/actuator/health` 与首页。

### 外部依赖与结论

- 可行：远端已具备 Ubuntu 22.04、Docker、Docker Compose、MySQL client，SSH key 登录可用。
- 修正事实：用户口述的 MySQL 密码不可用；远端当前应用已有可用数据库连接配置，生产部署已复用服务器侧 `.env`，未写入仓库。
- GitHub CLI 不存在；可通过本机 GitHub OAuth credential 调 GitHub API 配置 repository secrets。
- 生产首次容器启动失败原因：Spring Boot 4 需要 `spring-boot-starter-flyway` / `spring-boot-flyway` 才会执行 Flyway auto-configuration；仅引入 `flyway-core` 与 `flyway-mysql` 不会自动跑迁移，导致 V4/V5 未应用、JPA validate 报缺表。
- 已配置 GitHub repository secrets：`DEPLOY_HOST`、`DEPLOY_PORT`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`。
- 已在服务器 `/opt/memospark/.env` 写入生产环境变量，权限 `600`，未写入仓库。
- 交付状态：`memospark-app` 容器 healthy，`http://117.72.99.55:8080/` 返回 200。
