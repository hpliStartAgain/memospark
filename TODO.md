---
status: done
branch: main
owner: devin
updated: 2026-06-29 16:00
tier: COMPLEX
---

## 1. 需求理解

- **Bug 修复**：学习计划页点击「生成计划」按钮后前端后端均无反应。根因：前端 mutation 无 onError 且无 toast 组件，错误被静默吞掉；后端 controller/service 零日志，请求失败不可见。
- **日志增强**：后端 controller/service 日志极少，前端请求在后端看不到任何处理过程。需要加请求级日志 + 业务级日志。
- **Admin 后台面板**：不使用 prometheus+grafana，内置一个 admin 页面，查看实例基础信息（JVM/版本/启动时间）、DAU、用户数、复习量、牌组/卡片总量等。仅 ADMIN 角色可见。
- **未完成功能发掘**：扫描 TODO.md 旧条目与代码实际状态，补齐明显缺口。

## 2. 设计方案

### Bug 修复
- 前端：新增轻量 Toast 通知组件（不引入新依赖，基于 Zustand store + 固定容器），接入 generate mutation 的 onError；在 main.tsx QueryClient 加全局 error default handler。
- 后端：新增 RequestLoggingFilter（OncePerRequestFilter），记录每个 /api/** 请求的 method、path、status、耗时；StudyPlanController/Service 加 @Slf4j 业务日志。

### 日志增强
- 所有 controller 加 @Slf4j，关键写操作（POST/PUT/PATCH/DELETE）记录入参摘要和结果。
- 关键 service（StudyPlanService、ReviewService、AiService 已有、TargetService、DeckService）在异常路径和重要分支加 log.warn/error。
- application.properties 调整日志级别：com.memospark=DEBUG，并启用请求日志。

### Admin 面板
- 后端：AdminController（/api/admin/**，已受 SecurityConfig hasRole ADMIN 保护）+ AdminService。
  - 系统信息：JVM 版本、Spring Boot 版本、启动时间、运行时长、内存使用（ManagementFactory）。
  - 用户指标：总用户数、今日新增、DAU（通过 review_logs join card join deck join user 取 distinct user，按日）、最近注册用户列表。
  - 业务指标：总牌组、总卡片、总复习次数、总目标数、今日复习次数、保留率。
  - 近 30 天 DAU 趋势数据。
- 前端：AdminPage.tsx，卡片式布局展示上述指标 + DAU 折线图（用已有 recharts）。路由 /admin，仅 admin 用户可见（Layout 加条件 nav，AuthGuard 加角色检查）。
- DTO：AdminSystemInfoDto、AdminStatsDto、AdminDauPointDto。

### 未完成功能
- 旧 TODO.md 中 B 段多数已实现（PlansPage、DeckDetailPage、ReviewPage 均存在）。实际缺口：
  - 前端无任何错误提示能力（本次修复）。
  - 后端无请求日志（本次修复）。
  - 无 admin 管理能力（本次新增）。

## 3. 阶段划分

- [x] Phase 1: 后端日志基础设施 — RequestLoggingFilter + application.properties 日志级别 + controller/service 日志
- [x] Phase 2: 前端错误反馈 — Toast 组件 + store + generate mutation onError + 全局 error handler
- [x] Phase 3: Admin 后端 — AdminController + AdminService + DTO + repository 查询 + 用户管理 + DAU
- [x] Phase 4: Admin 前端 — AdminPage + 路由 + 导航 + 角色守卫
- [x] Phase 5: 验证 — 前端 build 通过；后端代码审查通过（本机无 Java 21 无法编译）

## 4. 文件级任务

| 文件 | 动作 | 说明 |
|------|------|------|
| src/main/java/.../config/RequestLoggingFilter.java | NEW | 请求日志过滤器 |
| src/main/resources/application.properties | MODIFY | 日志级别 |
| src/main/java/.../controller/StudyPlanController.java | MODIFY | 加 @Slf4j + 日志 |
| src/main/java/.../service/StudyPlanService.java | MODIFY | 加 @Slf4j + 业务日志 |
| src/main/java/.../controller/*.java | MODIFY | 关键 controller 加日志 |
| src/main/java/.../controller/AdminController.java | NEW | Admin API |
| src/main/java/.../service/AdminService.java | NEW | Admin 指标聚合 |
| src/main/java/.../dto/AdminDtos.java | NEW | Admin DTO |
| src/main/java/.../repository/ReviewLogRepository.java | MODIFY | 新增 DAU 查询 |
| src/main/java/.../repository/UserRepository.java | MODIFY | 新增注册统计查询 |
| src/main/java/.../config/SecurityConfig.java | MODIFY | /admin 路由 permitAll（SPA） |
| frontend/src/components/ui/Toast.tsx | NEW | Toast 通知组件 |
| frontend/src/store/toastStore.ts | NEW | Toast 状态管理 |
| frontend/src/main.tsx | MODIFY | 全局 error handler |
| frontend/src/pages/PlansPage.tsx | MODIFY | generate onError |
| frontend/src/pages/AdminPage.tsx | NEW | Admin 面板页面 |
| frontend/src/lib/api.ts | MODIFY | adminApi |
| frontend/src/App.tsx | MODIFY | /admin 路由 |
| frontend/src/components/Layout.tsx | MODIFY | admin nav 条件显示 |
| frontend/src/types/index.ts | MODIFY | Admin 类型 |

## 5. 外部变更（必须显式列出）

- [ ] 无数据库迁移（复用现有表结构，DAU 通过 join 查询）
- [ ] 无新增依赖（Toast 用 Zustand 已有；图表用 recharts 已有）
- [ ] 无新增环境变量
- [ ] 无配置文件新增

## 6. 待确认问题（@老板）

- Q1: Admin 面板是否需要用户管理能力（封禁/改角色/重置密码），还是只读监控即可？→ 默认只读监控，后续可扩展。
- Q2: DAU 定义是「当天有复习记录的独立用户」还是「当天有任何 API 请求的用户」？→ 默认前者（基于 review_logs，数据已有）。

## 7. 备选方案

- 方案 A（采用）：自建轻量 Toast + 自建 Admin 页面。优点：零新依赖、完全可控、风格统一；缺点：需手写 UI。
- 方案 B：引入 react-hot-toast + 复用 actuator metrics。优点：省代码；缺点：新依赖、actuator 暴露安全面增大。
