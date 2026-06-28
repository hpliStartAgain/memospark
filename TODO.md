---
status: done
branch: main
owner: devin
updated: 2026-06-28 19:05
tier: COMPLEX
---

## 1. 需求理解

- 遵循「视差滚动」风格提示的布局与交互动效规则，但**不改动既定配色（Claude Terracotta `#da7756` + Off-white）和字体（Inter + JetBrains Mono）**。
- 把首页改为左右布局或更精致 modern 的布局。
- 添加更多产品介绍内容。
- 更新项目 icon 使其更有设计感。
- 范围：新增公开 Landing 页 + 重构登录后 Dashboard 为左右两栏 + 全局基础组件升级动效 + icon/manifest 重新设计。

## 2. 设计方案

### 2.1 路由策略
- 新增 `/landing` 公开路由（产品介绍页，视差滚动全屏区块）。
- `/` 路由：未登录 → 重定向到 `/landing`；已登录 → Dashboard。
- `/login` 页面增加「返回首页」链接到 `/landing`。
- Landing 页 CTA：「立即登录」→ `/login`，「进入工作台」→ `/`（已登录则进 Dashboard）。

### 2.2 Landing 页（视差滚动风格）
- 全屏区块（`min-h-screen`）+ `bg-fixed` 背景图（CSS 渐变模拟，避免外部资源）创造深度视差。
- 区块节奏：Hero（左右布局：左文案+CTA，右产品视觉/就绪度环示意）→ 功能矩阵（卡片网格）→ 刷题+复习+面试就绪度三段产品介绍（左右交替）→ MCP/AI 集成介绍 → 数据统计 → CTA 收尾。
- 移动端 `bg-fixed` 降级为 `bg-local`（iOS Safari 不支持 fixed），通过媒体查询处理。
- 动效：`backdrop-blur-md rounded-full` 胶囊按钮、`backdrop-blur-sm rounded-2xl` 卡片、`hover:-translate-y-1`、`transition-all duration-300`、`active:scale-95`。
- 顶部导航栏：固定 + 滚动后 `backdrop-blur` 玻璃态。

### 2.3 Dashboard 重构（左右两栏）
- 左栏（`lg:col-span-2`）：问候语 + 主目标就绪度卡（含 ReadinessRing + 倒计时）+ 今日任务清单。
- 右栏（`lg:col-span-1`）：快速统计竖排卡（连续天数/今日复习/保留率）+ 薄弱技能 + 产品功能快捷入口（新增「产品介绍」模块，引导到 Landing 或直接展示功能）。
- 顶部加一行「备考进度条」横向统计带。

### 2.4 全局基础组件升级（保留 API，仅调样式/动效）
- `Card`：`rounded-xl` → `rounded-2xl`，加 `backdrop-blur-sm`，`hoverable` 动效升级为 `hover:-translate-y-0.5 hover:shadow-lg transition-all duration-300`。
- `Button`：加 `active:scale-95 transition-all duration-300`，hover 增加细微位移；保留现有 `rounded-md/lg` 圆角（避免 11 个页面按钮全部胶囊化破坏表单视觉），Landing 页内部用自定义胶囊按钮。
- `Input`：加 `backdrop-blur-sm transition-all duration-300`，focus 增加阴影；保留 `rounded-lg`。
- 不改 `Badge`、`Modal`、`Spinner`（风险/收益不匹配）。

### 2.5 Icon 重新设计
- 抛弃蓝色 `#0ea5e9` 圆角矩形 + 白闪电的旧方案（与 Terracotta 配色冲突）。
- 新方案：Terracotta 渐变圆角方形背景 + 白色「记忆火花」图形（闪电 + 重复循环弧线组合，呼应 SRS 间隔重复 + 火花点亮记忆的产品理念）。
- 同步更新 `favicon.svg`、`icon.svg`、`manifest.webmanifest`（`theme_color`/`background_color` 改为 Terracotta 系）、`index.html` 的 `theme-color`。

### 2.6 风格自检约束
- 禁止 `bg-scroll`、`rounded-none`、`shadow-none`。
- 禁止紫蓝渐变、渐变文字、bounce/elastic 缓动。
- 动效提供 `prefers-reduced-motion` 降级。
- 正文对比度 ≥ WCAG AA。
- 保留 Terracotta 配色与 Inter/JetBrains Mono 字体不变。

## 3. 阶段划分

- [x] Phase 1: 重新设计 icon + 更新 manifest/index.html theme-color
- [x] Phase 2: 升级基础组件 Card/Button/Input 动效
- [x] Phase 3: 新增 LandingPage（视差滚动产品介绍）
- [x] Phase 4: 重构 DashboardPage 为左右两栏 + 产品介绍模块
- [x] Phase 5: 路由接入 Landing + LoginPage 增加返回首页链接
- [x] Phase 6: 构建验证（tsc + vite build）+ 风格自检

## 4. 文件级任务

| 文件 | 动作 | 说明 |
|------|------|------|
| frontend/public/favicon.svg | MODIFY | 新设计图标 |
| frontend/public/icon.svg | MODIFY | 同 favicon |
| frontend/public/manifest.webmanifest | MODIFY | theme_color/background_color 改 Terracotta |
| frontend/index.html | MODIFY | theme-color 改 Terracotta |
| frontend/src/components/ui/Card.tsx | MODIFY | rounded-2xl + backdrop-blur-sm + hover 动效 |
| frontend/src/components/ui/Button.tsx | MODIFY | active:scale-95 + transition-all |
| frontend/src/components/ui/Input.tsx | MODIFY | backdrop-blur-sm + transition-all + focus 阴影 |
| frontend/src/pages/LandingPage.tsx | NEW | 视差滚动产品介绍页 |
| frontend/src/pages/DashboardPage.tsx | MODIFY | 重构为左右两栏 + 产品介绍模块 |
| frontend/src/App.tsx | MODIFY | 接入 /landing 路由 + / 重定向逻辑 |
| frontend/src/pages/LoginPage.tsx | MODIFY | 增加「返回首页」链接 |

## 5. 外部变更（必须显式列出）

- [ ] 无数据库迁移
- [ ] 无新增依赖（全部用现有 Tailwind + lucide-react）
- [ ] 无新增环境变量
- [ ] 无配置文件变更（仅 manifest/index.html 主题色）

## 6. 待确认问题（@老板）

- Q1: Landing 页是否需要中英双语？（现有项目有 i18n，默认按中文实现，i18n key 后续可补）
- Q2: Dashboard 右栏「产品功能快捷入口」是跳转到对应功能页，还是展示静态介绍？默认前者（快捷入口卡）。

## 7. 备选方案

- 方案 A（采用）：新增独立 Landing + 重构 Dashboard + 全局组件动效升级。覆盖最全，风险可控。
- 方案 B：仅重构 Dashboard 加入产品介绍。工作量小，但未登录访客无产品介绍入口，不符合「添加更多产品介绍」预期。
