# Changelog

## 2026-06-28 — JD 牌组命中复用 + 问答式 AI 复习闭环

- JD 技能分析改为先匹配用户已有 `CUSTOM` 牌组：综合技能名、牌组名、描述、标签和样例卡片内容打分，默认 `0.72` 以上自动复用，避免 Kubernetes/K8s 等同主题重复创建牌组。
- 新增 Flyway `V10__jd_deck_reuse_and_review_evidence.sql`：`target_skills` 增加 `deck_link_source` / `deck_match_score`，`review_logs` 增加用户答案与 AI 评审证据字段。
- 新增 `DeckLinkSource`：删除技能时只删除 `AI_CREATED` 牌组，`MATCHED_EXISTING` 复用牌组不会被误删；手动添加技能标记为 `MANUAL`。
- `TargetSkillDto` 增加牌组来源、命中牌组名和命中分；目标详情页展示“复用牌组 / AI 新建牌组 / 手动技能”状态。
- 新增复习问答 API：`POST /api/review/{cardId}/evaluate-answer` 结构化返回 `grade/quality/score/feedback/missingPoints/suggestedAnswer`；`POST /api/review/{cardId}/explain-answer` 支持用户围绕本题继续追问。
- 扩展 `ReviewRequest`，最终 SRS 提交可保存 `userAnswer`、`aiGrade`、`aiFeedback`、`aiSuggestedAnswer` 到 `review_logs`。
- 重构复习页为“先回答 → AI 评审 → 追问解释 → 确认质量分 → 进入下一张”，保留直接翻面/手动评分旁路，并支持把最终答案替换原卡片标准答案。
- 语音转文本暂不接入，仅在回答区预留禁用的语音按钮入口，不申请录音权限。

### Verification

- Passed: `frontend/ npm run build`（`tsc && vite build`）。
- Passed: `.\mvnw.cmd "-Dtest=TargetSkillServiceTest,ReviewServiceTest,AiServiceTest" "-Dskip.npm" "-Dfrontend.skip=true" test`（Tests run: 20, Failures: 0, Errors: 0）。
- Passed: `.\mvnw.cmd test`（Tests run: 56, Failures: 0, Errors: 0, Skipped: 1）。
- Note: 第一次全量 Maven test 在 Windows `target/classes/static/assets` 复制阶段遇到文件占用；清理该构建输出目录后重跑通过。

## 2026-06-28 — 前端视差滚动改版 + 产品介绍页 + Icon 重设计

- 新增公开产品介绍页 `/landing`：视差滚动全屏区块布局（Hero 左右布局 + 功能矩阵 + 工作流左右交替 + AI/MCP 集成 + 数据统计 + CTA），未登录访问 `/` 自动跳转 Landing，登录后进 Dashboard。
- 重构 Dashboard 为左右两栏：左栏主目标就绪度 + 今日任务，右栏学习数据 + 薄弱技能 + 产品功能快捷入口；顶部新增备考进度带。
- 全局基础组件动效升级：Card 改 `rounded-2xl + backdrop-blur-sm + hover 位移`，Button 加 `active:scale-95 + transition-all`，Input 加 `backdrop-blur-sm + focus 阴影`；保留 Terracotta 配色与 Inter/JetBrains Mono 字体不变。
- 重新设计项目 Icon：Terracotta 渐变背景 + 白色「记忆火花（闪电）+ SRS 同心弧线」组合，替换原蓝色闪电；同步更新 favicon.svg、icon.svg、manifest 主题色、index.html theme-color。
- 视差动效提供 `prefers-reduced-motion` 降级与移动端 `background-attachment: local` 降级。
- LoginPage 增加返回首页链接，登录成功跳转改为 Dashboard。

## 2026-06-28 — AI 设置 + P3/P4 交付收口

- 新增用户级 AI 设置：`/api/settings/ai` 支持供应商、Base URL、模型、API Key 保存与连接测试；API Key 加密落库，接口只返回脱敏状态。
- 通过生产 SQL 直接为 `hpli` 初始化技术面试牌组：按工作职责抽象为 K8s、大数据、网络、Linux、JVM、Go、可观测告警、分布式高可用 8 个牌组；保留 Java/算法/LeetCode，清理无关英语牌组。
- 补齐微信小程序生产构建/上传前校验脚本：要求显式 `TARO_APP_API_URL`、拒绝线上 HTTP/裸 IP、扫描产物中的 localhost，并支持通过 `project.private.config.json` 与 `WECHAT_DEVTOOLS_CLI` 准备上传。
- 默认 AI 供应商切到 SenseNova-compatible：`https://token.sensenova.cn/v1` + `deepseek-v4-flash`，同时保留服务端环境变量兜底。
- `AiService` 接入用户级配置，JD 分析、技能成卡、面经成卡、模拟面试出题/评分等生产路径均使用当前用户配置。
- 新增 gated `AiLiveE2ETest`，在显式提供运行时 key 时覆盖连接、JD 分析、成卡、模拟面试出题和答案点评。
- 完成 P3 剩余项：求职漏斗看板、目标状态流转、错题本一键转卡、统计页复习趋势与目标就绪度。
- 完成 P4 关键优化：路由懒加载、Vite manual chunks、复习页进度/连击/预计剩余时间/键盘评分；修复数字评分键在不同浏览器事件下不稳定的问题。
- 修复统计页趋势图保留率二次乘 100 的显示错误。
- 修复 SPA 构建资源同步：Maven 生命周期显式复制 Vite 产物到 `target/classes/static`，避免 `spring-boot:run`/package 运行态缺 `index.html`。
- 修复复习提交链路：强制生成 SPA 可读 CSRF cookie，并将复习页提交字段从错误的 `card.id` 对齐为后端 DTO 的 `cardId`。
- 修复小程序 API 层：移除生产包中的 `localhost` 兜底，未配置 `TARO_APP_API_URL` 时请求直接报错，避免线上构建残留开发地址。

### Verification

- Passed: `.\mvnw.cmd test`（Tests run: 49, Failures: 0, Errors: 0, Skipped: 1）。
- Passed: `frontend/ npm run build`（`tsc && vite build`，无 >500kB chunk 警告）。
- Passed: live AI E2E（运行时 key 仅通过环境变量提供，未写入仓库）。
- Passed: 本地浏览器 E2E：设置页 AI 测试连接、目标漏斗 Offer 阶段、统计页目标就绪度、复习页点击评分与键盘评分。
- Passed: production SQL verification for `hpli` decks -> 11 decks total, new 8 custom technical decks, 188 card progress rows.
- Passed: `miniprogram/ npm run build:weapp` 与 `TARO_APP_API_URL=https://memospark.example.com npm run build:weapp:prod`；上传校验的 built output 检查通过。
- Expected blocker: `miniprogram/ npm run validate:weapp` 在未提供真实 AppID/微信开发者工具 CLI 时失败，证明上传门禁有效。

## 2026-06-28 — P3 partial：面经/笔记一键成卡

- 新增 `POST /api/decks/{id}/cards/from-text`：粘贴面经、笔记或项目材料后，复用 `AiService.generateCards` 抽取原子化问答卡片并落到指定牌组。
- `CardService.generateCardsFromText` 复用既有 `Card + CardProgress + initProgress` 范式，生成的新卡当天可进入复习。
- 牌组页新增「成卡」入口和弹窗，可选择生成数量并粘贴原文；生成后刷新牌组卡片数。

### Verification

- Passed: `.\mvnw.cmd test`（Tests run: 48, Failures: 0, Errors: 0）。
- Passed: `frontend/ npm run build`（`tsc && vite build`）。
- 待真实 AI 验证：粘贴面经/笔记 → 生成卡片 → 进入对应牌组复习。

## 2026-06-28 — P2：AI 模拟面试 MVP

- 新增模拟面试领域模型与 Flyway `V8__mock_interview.sql`：`mock_interviews` 记录会话，`mock_interview_questions` 记录逐题题目、回答、评分和反馈。
- `AiService` 新增 `generateInterviewQuestions` 和 `evaluateInterviewAnswer`：按目标 JD/技能生成 STAR/技术/系统设计题，逐题对文字回答给出结构化评分与反馈。
- 新增 `MockInterviewService` / `MockInterviewController`，提供 `/api/targets/{targetId}/mock-interviews/**`：列表、开始一场、取详情、逐题作答、完成会话。
- `ReadinessService` 新增 `mockPerformance`：近 60 天已完成模拟面试平均分会纳入 overall（有模拟成绩时权重 20%；没有成绩时沿用 P1 权重，避免误伤新用户）。
- 前端新增 `/targets/:id/mock` 模拟面试页：选择类型/题数、启动面试、逐题回答、查看 AI 点评、历史记录与本场平均分；目标详情页新增「模拟面试」入口和就绪度分项。
- 新增 `MockInterviewServiceTest` 覆盖逐题评分后自动汇总并完成会话的核心逻辑。

### Verification

- Passed: `.\mvnw.cmd test`（Tests run: 48, Failures: 0, Errors: 0）。
- Passed: `frontend/ npm run build`（`tsc && vite build`）。
- 待手动/部署后验证：真实 `AI_API_KEY` 下从目标页进入模拟面试 → 生成题目 → 作答 → AI 点评 → 就绪度 `mockPerformance` 更新。

## 2026-06-28 — P1：就绪度可信化 + FSRS

- 新增 Flyway `V7__fsrs_and_evidence.sql`：`card_progress` 增加 `stability` / `difficulty`，`srs_settings` 增加 `desired_retention`，`review_logs` 增加 FSRS undo 快照字段。
- 新增 `FsrsEngine`，用 stability / difficulty / desired retention 计算复习间隔；`SpacedRepetitionService` 的卡片复习调度切到 FSRS，同时保留并更新旧 `easeFactor` / `interval` 字段作为兼容输出。
- `ReviewService` 的撤销复习补齐 `stability` / `difficulty` 恢复，避免 undo 后调度状态不完整。
- `ReadinessService.skillCoverage` 从纯自评改为 evidence-aware：当目标技能有关联 deck 且近 30 天有复习记录时，按「自评 50% + 复习正确率 50%」折算技能掌握度；没有复习证据时继续用自评兜底。
- 设置页和 SRS DTO/API 增加 `desiredRetention`，默认 `0.9`，后端约束到 `0.7..0.98`。
- 新增 `FsrsEngineTest`，并补充 `ReviewServiceTest` 覆盖 FSRS undo 快照。

### Verification

- Passed: `.\mvnw.cmd test`（Tests run: 47, Failures: 0, Errors: 0）。
- Passed: `frontend/ npm run build`（`tsc && vite build`）。
- 已知提醒：前端仍有 Vite `chunk >500kB` 警告，归入 P4 性能优化。

## 2026-06-28 — P0：接通 JD→技能→牌组→卡片→复习闭环

- `target_skills` 新增 `deck_id` / `topics` / `suggested_card_count`（Flyway `V6`），把每个目标技能关联到一个可复习的学习牌组。
- `TargetSkillService` 不再丢弃 AI 返回的 `decks`：分析 JD 时为每个技能创建（空）学习牌组并保存 `topics`/建议卡数；新增 `generateCardsForSkill` 按需（每技能 1 次 AI 调用）经 `AiService.generateCards` 落库卡片并初始化 `CardProgress`（当天可复习）。
- 删除技能/目标或重新分析（replace）时连带清理其生成的牌组（卡片 + 进度 + 复习日志）。
- 新增端点 `POST /api/targets/{id}/skills/{skillId}/generate-cards`；`TargetSkillDto` 输出 `deckId`/`cardCount`。
- 前端：技能行支持「生成卡片」与「复习此技能(n) → /review/:deckId」，技能区新增「一键生成全部」；驾驶舱「提升薄弱技能」在有卡时直达对应牌组复习（不再是死路）。
- 生成策略选「按需」而非分析时同步全量，避免多次串行 AI 调用导致请求超时，并控制单人自用的 AI 成本。

### Verification

- Passed: `.\mvnw.cmd test`（Tests run: 42, Failures: 0, Errors: 0）。
- Passed: `npm run build`（`tsc && vite build`）。
- 待手动/部署后验证：建目标 → 加 JD → AI 分析 → 生成卡片 → `/review/:deckId` 复习（需配置 `AI_API_KEY`）。

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
