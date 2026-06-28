# EVIDENCE（历史证据；接手 Agent 按风险决定是否重跑）

## 验证（2026-06-28 Codex 收口）
- hpli 技术面试牌组：通过 SSH 在生产库执行 `.agent/handoff/20260628-1425-memospark-maturation/hpli_interview_decks_seed.sql`。
  - 查库验证：保留 `Java Programming`、`Algorithms & Data Structures`、`LeetCode Hot 100`；`English Vocabulary` 已清理。
  - 新增 8 个 CUSTOM 技术面试牌组：Kubernetes、大数据、计算机网络、Linux、JVM、Golang、可观测告警、分布式高可用；每组 6 张卡，共 48 张。
  - `card_progress` 总数为 188。
- 微信小程序：`npm run build:weapp` 通过；`TARO_APP_API_URL=https://memospark.example.com npm run build:weapp:prod` 通过。
  - `npm run validate:weapp` 的 API URL 与 built output 检查通过；当前仅因缺少真实 AppID/微信开发者工具 CLI 按预期失败。
  - 小程序 API 层已移除 `localhost` 兜底，构建产物不残留开发地址。
- 后端最终：`.\mvnw.cmd test` → `Tests run: 49, Failures: 0, Errors: 0, Skipped: 1`，`BUILD SUCCESS`。
  - `AiLiveE2ETest` 默认跳过；需 `LIVE_AI_E2E=true` 与运行时 key 才执行。
  - `AiServiceTest` 中 localhost `ConnectException` 堆栈是预期的失败兜底测试，测试结果为通过。
- 前端最终：`frontend/ npm run build` → `tsc && vite build` exit 0；manual chunks 后无 `chunk >500kB` 警告。
- Maven 运行态资源：`.\mvnw.cmd -DskipTests process-classes` → `target/classes/static/index.html` 存在，`spring-boot:run` 根页面返回 200。
- Live AI E2E：`AiLiveE2ETest` 运行通过，覆盖连接测试、JD 分析、成卡、模拟面试出题和答案点评；运行时 key 未写入仓库/交接包。
- 本地浏览器 E2E：
  - 设置页 AI 测试连接显示 `连接成功：OK`。
  - 目标漏斗看板显示目标处于 Offer 阶段。
  - 统计页显示目标岗位就绪度与复习趋势。
  - 复习页点击「不错」从 `1 / 19` 推进到 `2 / 19`，`Space` + `4` 推进到 `3 / 19`，连击更新到 2。
- 安全/构建修复验证：
  - 本地 HTTP session：携带 SPA `XSRF-TOKEN` 后 `POST /api/review/{cardId}` 返回 200。
  - `rg` 确认最终静态资源里没有 `retentionRate*100` 旧逻辑。
  - `git diff --check` 无空白错误（仅 CRLF 提示）。
  - secret scan 未命中用户提供的 AI key 或明文 `AI_API_KEY`。

## 验证（2026-06-28）
- P3 partial 后端：`.\mvnw.cmd test` → `Tests run: 48, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
- P3 partial 前端：`frontend/ npm run build`（`tsc && vite build`）→ exit 0。
  - 注：vite 仍警告 chunk >500kB，归入 P4 性能优化。
- P2 后端：`.\mvnw.cmd test` → `Tests run: 48, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
- P2 前端：`frontend/ npm run build`（`tsc && vite build`）→ exit 0。
  - 注：vite 仍警告 chunk >500kB，归入 P4 性能优化。
- P1 后端：`.\mvnw.cmd test` → `Tests run: 47, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
- P1 前端：`frontend/ npm run build`（`tsc && vite build`）→ exit 0。
  - 注：vite 仍警告 chunk >500kB，归入 P4 性能优化。
- 后端：`.\mvnw.cmd test` → `Tests run: 42, Failures: 0, Errors: 0`，`BUILD SUCCESS`。
  - 注：日志里 `AiServiceTest` 有 `ConnectException` 报错是**预期**——它故意打 localhost 验证 AI 失败兜底路径，测试本身通过。
- 前端：`frontend/ npm run build`（`tsc && vite build`）→ exit 0。
  - 注：vite 警告 chunk >500kB（Monaco 等），P4 处理。

## 关键代码发现（支撑诊断，避免 Codex 重新调研）
- **闭环原断点**：`service/TargetSkillService.java`（改前）只取 `mapWeight(suggestedCardCount)`，丢弃 AI 返回的 `topics` 与牌组——`analyzeJds` 本就返回 `{"decks":[{name,description,topics,suggestedCardCount}]}`（见 `service/AiService.java:182-221`）。
- **造卡能力本就存在**：`service/AiService.java:152-173 generateCards` + `:227 generateCardsForTopic`。
- **建可复习牌组范式**：`service/DeckService.java:110-129 copyPoolDeck`（Deck→Card→CardProgress→`srsService.initProgress`，新卡当天到期）。
- **就绪度公式**：`service/ReadinessService.java:66` → `overall = 0.5*skillCoverage + 0.3*cardHealth + 0.2*wrongClear`；`skillCoverage` 纯自评（`:39-50`）。
- **SRS 现状**：`service/SrsEngine.java` 纯 SM-2；`SpacedRepetitionService.applyReview:34-66`。FSRS 数据基础：`domain/ReviewLog.java` 已记录复习历史。
- **测试环境**：`src/test/resources/application.properties` → H2 `MODE=MySQL` + `ddl-auto=create-drop` + `flyway.enabled=false`（实体即 schema 源）。
- **P1 已改**：`service/FsrsEngine.java` 新增 FSRS/DSR 调度；`SpacedRepetitionService` 卡片复习切到 FSRS；`ReadinessService` 在技能 deck 有近 30 天复习记录时用自评/正确率 50/50 混合计算 `skillCoverage`。
- **P2 已改**：`service/MockInterviewService.java` + `controller/MockInterviewController.java` 串起目标上下文出题、逐题作答、AI 评分和会话汇总；`ReadinessService` 用近 60 天完成模拟面试平均分计算 `mockPerformance`。
- **P3 partial 已改**：`DeckController` 新增 `/api/decks/{id}/cards/from-text`；`CardService.generateCardsFromText` 从面经/笔记抽卡并初始化 `CardProgress`；牌组页新增「成卡」弹窗。

## pm-skills 安装证据
- `python scripts/lint_skills.py` → 0 error；`registry_sync.py status` → codex/claude/devin `SYNCED=58`（原 42 + 新 16），gemini 42 不变，pi 是安装前就有的 `MISSING DIR`（不在范围）。
- 备份：`agent-skill-registry/backups/sync-20260628-140028/`。
