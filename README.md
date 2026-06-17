# MemoSpark

基于间隔重复（SRS）的记忆与刷题练习平台，包含内置 LeetCode Hot 100 题库、闪卡复习、代码评测、AI 辅助，以及可接入 AI 助手（Claude Desktop / Windsurf 等）的 MCP Server。

**后端**：Spring Boot 4.0.5 · Java 17 · Spring Data JPA · Spring Security · Flyway · MySQL 8 · Lombok · Jackson  
**前端**：React 18 + TypeScript · Vite · Tailwind CSS · Zustand  
**MCP Server**：Node.js 18+ · TypeScript · `@modelcontextprotocol/sdk`

## 目录

- [快速开始](#快速开始)
- [启动方式](#启动方式)
- [配置项](#配置项)
- [Quick-Add API 与 MCP Server](#quick-add-api-与-mcp-server)
- [启动初始化与性能说明](#启动初始化与性能说明)
- [常用 Make 目标](#常用-make-目标)
- [项目结构](#项目结构)

## 快速开始

前置依赖：

- JDK 17+
- Maven Wrapper（已自带 `mvnw` / `mvnw.cmd`）
- MySQL 8（本地不存在数据库时会自动创建）
- Node.js 18+（仅使用 MCP Server 时需要）
- 可选：`make`（Windows 可用 GnuWin / WSL / Git Bash）

最小启动：

```bash
# 必填环境变量
export DB_PASSWORD=root
export AI_API_KEY=your_llm_key

# 可选（有默认值）
export DB_USERNAME=root
export DB_URL="jdbc:mysql://localhost:3306/memospark?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&rewriteBatchedStatements=true"

make run
```

启动后访问 <http://localhost:8080>，默认管理员：`admin / admin123`。

## 启动方式

| 场景 | 命令 |
| ---- | ---- |
| 本地开发热启动 | `make run` |
| 打包可执行 jar | `make package` |
| 运行打包后的 jar | `make run-jar` |
| 单元测试 | `make test` |
| 清理构建产物 | `make clean` |

底层等价命令（无 `make` 时直接用）：

```bash
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
./mvnw -DskipTests package
java -jar target/memospark-0.0.1-SNAPSHOT.jar
./mvnw test
```

## 配置项

所有外部依赖均通过环境变量注入。**加粗**为必填项（无默认值）。

### 数据库

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `DB_URL` | `jdbc:mysql://localhost:3306/memospark?...` | MySQL JDBC URL；保留 `rewriteBatchedStatements=true` 启用批量写入 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| **`DB_PASSWORD`** | _(必填)_ | MySQL 密码 |

### AI 服务（OpenAI 兼容接口）

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| **`AI_API_KEY`** | _(必填)_ | LLM API Key |
| `AI_API_URL` | `https://api.deepseek.com/v1/chat/completions` | LLM 接口地址（支持 DeepSeek / OpenAI / 阿里云等兼容格式）|
| `AI_API_MODEL` | `deepseek-chat` | 模型名 |
| `AI_TIMEOUT_SECONDS` | `30` | 单次请求超时（秒）|
| `AI_MAX_RETRIES` | `2` | 失败后最大重试次数（指数退避）|

### 代码评测

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `JUDGE_TYPE` | `judge0` | 评测后端：`judge0`（云端）或 `container`（本地 Docker）|
| `JUDGE0_URL` | `https://ce.judge0.com` | Judge0 服务地址 |
| `JUDGE0_KEY` | _(空)_ | Judge0 API Key |
| `JUDGE0_HOST` | _(空)_ | RapidAPI Host（使用 RapidAPI 路由时填写）|
| `JUDGE_JAVA_IMAGE` | `openjdk:17-slim` | 容器评测 Java 镜像（`JUDGE_TYPE=container`）|
| `JUDGE_PYTHON_IMAGE` | `python:3.11-slim` | 容器评测 Python 镜像 |
| `JUDGE_TIMEOUT` | `10` | 容器评测超时（秒）|
| `JUDGE_MEMORY_MB` | `256` | 容器评测内存限制（MB）|

### Quick-Add API / MCP

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `MEMOSPARK_API_KEY` | _(空，禁用)_ | Bearer Key，设置后启用 `/api/quick-add/**` 端点 |

### 安全 / 登录限流

| 环境变量 | 默认值 | 说明 |
| -------- | ------ | ---- |
| `LOGIN_MAX_ATTEMPTS` | `5` | 账号连续失败锁定阈值 |
| `LOGIN_LOCKOUT_MINUTES` | `15` | 锁定时长（分钟）|

### JPA / Hibernate 关键配置

- `ddl-auto=validate`：Flyway 管理 schema，Hibernate 仅校验
- `batch_size=50` + `order_inserts/updates=true`：启用 JDBC 批量写入
- `open-in-view=false`：关闭视图层懒加载

## Quick-Add API 与 MCP Server

在 AI 对话中遇到值得复习的概念时，可以通过 MCP 工具或直接调用 REST API 把卡片加入题库。

### 启用后端

```bash
export MEMOSPARK_API_KEY=your_secret_key
```

### MCP Server 安装

```bash
cd mcp-server
npm install
npm run build
```

在 Claude Desktop / Windsurf 的 MCP 配置文件中添加：

```json
{
  "mcpServers": {
    "memospark": {
      "command": "node",
      "args": ["C:/path/to/memospark/mcp-server/dist/index.js"],
      "env": {
        "MEMOSPARK_URL": "http://localhost:8080",
        "MEMOSPARK_API_KEY": "your_secret_key",
        "MEMOSPARK_USERNAME": "your_username"
      }
    }
  }
}
```

### 可用 MCP 工具

| 工具 | 说明 |
| ---- | ---- |
| `add_flashcard` | 向指定牌组添加闪卡（牌组不存在则自动创建）|
| `list_decks` | 列出所有牌组（含 ID）|
| `get_deck` | 获取牌组详情（卡片数、今日到期、限额）|
| `create_deck` | 创建新牌组 |
| `update_deck` | 修改牌组名称/描述/每日限额 |
| `delete_deck` | 删除牌组及全部卡片（不可撤销）|
| `list_cards` | 列出牌组内所有卡片及 SRS 状态 |
| `get_stats` | 总体学习统计（总卡数、连续天数、留存率）|
| `get_due_cards` | 今日待复习卡片（可按牌组过滤）|

### REST API 直接调用示例

```bash
# 查看牌组
curl -H "Authorization: Bearer <key>" \
  "http://localhost:8080/api/quick-add/decks?username=alice"

# 添加闪卡
curl -X POST -H "Authorization: Bearer <key>" \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","deckName":"AI问答","front":"Q?","back":"A!","tags":"ai"}' \
     "http://localhost:8080/api/quick-add/card"

# 学习统计
curl -H "Authorization: Bearer <key>" \
  "http://localhost:8080/api/quick-add/stats?username=alice"
```

完整端点列表见 `mcp-server/README.md`。

## 启动初始化与性能说明

启动期由两个 `ApplicationRunner` 完成数据准备：

1. `AdminInitializer`（`@Order(0)`）—— 首次启动创建默认 admin 账号并复制内置牌组
2. `CodeProblemDataInitializer`（`@Order(1)`）—— 把 17 类共 100+ 道 LeetCode Hot 100 题目同步进 `code_problems` 表

> **已优化：** 启动稳态下最多两次查询：
> - 一次 `select problem_number from code_problems` 投影查询，构造 `Set<Integer>` 做存在性判断；
> - 仅 `category` 缺失时，再发一次条件查询回填。
>
> 首次填充走 `saveAll(...)` + JDBC batch（`batch_size=50`，URL 携带 `rewriteBatchedStatements=true`）。

## 常用 Make 目标

```bash
make clean package           # 干净打包
make run                     # 开发态启动
make test                    # 跑单元测试
DB_PASSWORD=secret make run  # 临时覆盖环境变量
make help                    # 查看所有目标
```

## 项目结构

```
memospark/
├── src/main/java/com/memospark/core/
│   ├── MemosparkApplication.java        # 启动入口 + ObjectMapper Bean
│   ├── config/                          # Security / Web / CORS 等配置
│   ├── controller/
│   │   ├── QuickAddController.java      # Quick-Add API（Bearer 鉴权）
│   │   ├── DeckController.java
│   │   ├── ReviewController.java
│   │   ├── AiController.java
│   │   ├── PracticeController.java      # 代码题评测
│   │   └── ...
│   ├── domain/                          # JPA 实体（User、Deck、Card、CodeProblem...）
│   ├── dto/                             # 请求 / 响应 DTO
│   ├── init/                            # 启动期初始化
│   │   ├── AdminInitializer.java
│   │   ├── CodeProblemDataInitializer.java   # 批量 + 短路优化
│   │   └── BuiltinDataInitializer.java
│   ├── repository/                      # Spring Data JPA 仓库
│   └── service/
│       ├── SrsEngine.java               # SM-2 算法统一实现
│       ├── SpacedRepetitionService.java # 闪卡 SRS（复用 SrsEngine）
│       ├── ProblemNoteService.java      # 题目笔记 SRS（复用 SrsEngine）
│       ├── AiService.java               # AI 调用（重试 + 超时 + 注入 ObjectMapper）
│       ├── JudgeOrchestrator.java       # 并行多测试用例评测 + 状态聚合
│       ├── ReviewService.java
│       ├── DeckService.java
│       ├── CardService.java
│       └── ...
├── src/main/resources/
│   ├── application.properties           # 主配置（全量环境变量）
│   └── db/migration/                    # Flyway SQL 迁移脚本
├── frontend/                            # React 18 + TypeScript + Vite + Tailwind
│   └── src/
│       ├── pages/                       # 各功能页面
│       ├── store/                       # Zustand 状态管理
│       └── api/                         # API 请求层
└── mcp-server/                          # MCP Server（AI 助手集成）
    ├── src/index.ts                     # 9 个 MCP 工具
    ├── package.json
    └── README.md                        # MCP 配置与使用说明
```

## 许可证

仅供学习交流，未指定开源许可证。
