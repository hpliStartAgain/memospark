# TASK — MemoSpark 成熟化（交接 Codex）

## 目标
把 MemoSpark 从「个人 demo 观感」打磨成「即使只有老板一个人用也极好用、能真正帮他拿 offer」的产品。不追求面向大众的 SaaS；北极星是**证据化面试就绪度**，不是 DAU。

## 完成定义（整体）
- 闭环跑通且可信：JD → 技能 → 牌组 → 卡片 → 复习 →（模拟面试）→ **用证据度量就绪度** → 重新规划。
- 每个阶段：`./mvnw.cmd test` 绿 + `frontend/ npm run build` 绿 + 关键路径手动/部署后 E2E 通过。

## 范围（已规划，详见 `TODO.md` 的「成熟化 Roadmap」）
- P0 接闭环 —— **已完成**。
- P1 就绪度可信化 + FSRS —— **已完成**。
- P2 AI 模拟面试 MVP —— **已完成**。
- AI 设置可配置 + live AI E2E —— **已完成**。
- P3 内容供给 + 求职漏斗 + 统计升级 —— **已完成**。
- P4 前端关键优化/美化 —— **关键交付项已完成**，完整 i18n/a11y/品牌 polish 可后续继续。

## 硬约束（老板既定）
1. **commit / push / 部署前必须经老板确认**（本次改动尚未提交）。
2. AI 调用一律按需/逐条，避免同步多次串行导致请求超时。
3. secrets 只走环境变量（`AI_API_KEY` 等），不写进代码/交接包。
4. 测试用 H2 + `ddl-auto=create-drop` 且 `spring.flyway.enabled=false` → **新增实体字段必须与 Flyway 迁移列名/类型/nullability 一致**。
5. 不做实时面试外挂（伦理），只做赛前训练。

## 关键事实源
- 策略全文：`docs/PRODUCT_STRATEGY.md`
- 执行清单：`TODO.md` →「成熟化 Roadmap（交接 Codex）」
- 变更记录：`CHANGELOG.md`（顶部 AI/P3/P4 收口段）
- 验证记录：`.agent/handoff/20260628-1425-memospark-maturation/EVIDENCE.md`
