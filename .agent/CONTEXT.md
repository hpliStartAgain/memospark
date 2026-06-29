# 任务背景

用户反馈学习计划页「生成计划」按钮点击后前端后端均无反应。排查发现根因是前端 mutation 无 onError 且无 toast 组件，错误被静默吞掉；后端 controller/service 零日志。同时用户要求加日志、建 admin 后台面板（不用 prometheus+grafana）。

## 已完成

1. **Bug 修复**：Toast 组件 + 全局 mutation onError + PlansPage 成功 toast
2. **日志增强**：RequestLoggingFilter（每请求 method/path/status/耗时）+ GlobalExceptionHandler 日志 + StudyPlan 日志
3. **Admin 面板**：Flyway V12（users.enabled + user_daily_activity 表）+ AdminController/Service（系统信息/DAU/用户管理）+ AdminPage 前端

## 关键决策

- DAU 定义为「当天有任意 API 请求的独立用户」，通过 RequestLoggingFilter 在每个认证请求成功后写入 user_daily_activity 表。
- Admin 面板包含用户管理写操作（封禁/改角色/重置密码），不只是只读监控。
- Toast 用 Zustand 自建，不引入 react-hot-toast 等新依赖。
- User.enabled 字段通过 Spring Security 7 参数构造器实现封禁用户无法登录。

## 待验证

- 后端编译需 Java 21（本机仅 Java 17）。在生产环境或 CI 中运行 `./mvnw test` 验证。
- Flyway V12 迁移在 MySQL 8.0 上需验证（语法已审查）。

## 关键文件

- `src/main/java/com/memospark/core/config/RequestLoggingFilter.java` — 请求日志 + DAU 记录
- `src/main/java/com/memospark/core/controller/AdminController.java` — Admin API
- `src/main/java/com/memospark/core/service/AdminService.java` — Admin 指标聚合
- `frontend/src/pages/AdminPage.tsx` — Admin 前端页面
- `frontend/src/components/ui/Toast.tsx` — Toast 通知组件
- `src/main/resources/db/migration/V12__admin_user_activity.sql` — 数据库迁移
