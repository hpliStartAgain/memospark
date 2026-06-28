# DECISIONS

- **pm-skills 安装范围**：选「核心子集 16 个」（非全量 46），按治理规范进 `agent-skill-registry/skills/community/` 再同步 codex/claude/devin。理由：保持注册表精简，只装与「打磨产品 + 找工作」相关的 PM 框架。
- **P0 卡片生成时机**：选「**按需生成**」（每技能 1 次 AI 调用 / 前端「一键生成全部」逐个触发），而非 analyze 时同步全量或异步后台。理由：analyze 是同步请求，5-10 次串行 AI 调用会超时；按需可控成本，适合单人自用。
- **生成牌组类型**：复用 `DeckType.CUSTOM` + `user=target.user`，自动进入「我的牌组」，不新增枚举值，避免改动前端牌组分组渲染。技能↔牌组经 `TargetSkill.deck` 关联。
- **牌组清理**：删除技能/目标、或 analyze `replace=true` 时，连带删除生成牌组（卡片+进度+日志），避免孤儿。FK `ON DELETE SET NULL` 兜底。
- **P0 不动就绪度公式**：`skillCoverage` 仍自评；evidence 化留到 P1（此时 skill 已有 deck，可用复习正确率驱动）。
- **P1 evidence 权重**：技能有关联 deck 且近 30 天有复习记录时，`skillCoverage` 用「自评 50% + 复习正确率 50%」；没有 evidence 时继续自评兜底，避免新用户就绪度归零。
- **P1 FSRS 接入范围**：卡片复习调度切到 FSRS/DSR；错题本暂时继续用旧 `SrsEngine`，因为错题没有完整 `CardProgress`/`ReviewLog` 记忆状态，避免扩大改动面。
- **P1 兼容策略**：保留并继续更新 `easeFactor` / `review_interval` 字段；新增 `stability` / `difficulty` / `desired_retention` 作为 FSRS 状态与设置。
- **P2 AI 调用策略**：开始一场面试时生成题目；作答时逐题评分，避免一场面试同步串行多次点评导致超时。
- **P2 就绪度权重**：有已完成模拟面试时，overall = 技能覆盖 40% + 卡片健康 25% + 错题清空 15% + 模拟面试 20%；没有模拟成绩时沿用 P1 权重，避免新用户被空 evidence 惩罚。
- **P2 安全边界**：只做赛前训练，不做实时面试外挂；提示词中明确禁止 real-time cheating。
- **北极星**：「每周证据化就绪度净增」（productivity game），不是 DAU/MAU。
- **不做实时面试外挂**（Final Round 式 stealth copilot），伦理风险；只做赛前训练。
- **未提交/未部署**：遵循老板「commit/push/部署需确认」约束。
