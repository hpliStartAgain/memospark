# MemoSpark MCP Server

通过 MCP 协议将 AI 对话工具（Claude Desktop、Windsurf 等）接入 MemoSpark，  
一键把对话中的好问题加入闪卡题库。

## 工具清单

### 闪卡写入
| 工具 | 说明 |
|------|------|
| `add_flashcard` | 向指定牌组添加闪卡（牌组不存在则自动创建）|

### 牌组管理
| 工具 | 说明 |
|------|------|
| `list_decks` | 列出当前用户所有牌组（含 ID）|
| `get_deck` | 获取单个牌组详情（卡片数、今日到期、限额）|
| `create_deck` | 创建新牌组（可设每日新卡/复习上限）|
| `update_deck` | 修改牌组名称、描述或每日限额 |
| `delete_deck` | 删除牌组及其所有卡片（不可撤销，调用前会确认）|

### 只读查询
| 工具 | 说明 |
|------|------|
| `list_cards` | 列出牌组内所有卡片（正/反面、标签、SRS 状态）|
| `get_stats` | 总体学习统计（总卡数、连续天数、留存率）|
| `get_due_cards` | 今日待复习卡片（可按牌组过滤）|

## 快速上手

### 1. 配置后端 API Key

在 MemoSpark 服务端设置环境变量：

```bash
MEMOSPARK_API_KEY=your_secret_key_here
```

该 key 用于鉴权 `/api/quick-add/**` 接口，不需要 session/CSRF。

### 2. 安装依赖 & 构建

```bash
cd mcp-server
npm install
npm run build
```

### 3. 配置 MCP 客户端

在 Claude Desktop (`claude_desktop_config.json`) 或 Windsurf MCP 配置中添加：

```json
{
  "mcpServers": {
    "memospark": {
      "command": "node",
      "args": ["C:/path/to/memospark/mcp-server/dist/index.js"],
      "env": {
        "MEMOSPARK_URL": "http://localhost:8080",
        "MEMOSPARK_API_KEY": "your_secret_key_here",
        "MEMOSPARK_USERNAME": "your_username"
      }
    }
  }
}
```

### 4. 在对话中使用

对 AI 说：

> "把这道题加到我的 Java面试 牌组：正面是'什么是 volatile？'，背面是'…'"

AI 会自动调用 `add_flashcard`，返回：

```
✓ Flashcard added successfully!
  Deck : Java面试 (id=3)
  Card : #42
  Front: 什么是 volatile？
  Back : ...
```

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `MEMOSPARK_URL` | MemoSpark 服务地址 | `http://localhost:8080` |
| `MEMOSPARK_API_KEY` | 与服务端 `MEMOSPARK_API_KEY` 一致 | *(必填)* |
| `MEMOSPARK_USERNAME` | 操作目标用户名 | *(必填)* |

## 后端 API 直接调用（无 MCP）

```bash
# 查看牌组
curl -H "Authorization: Bearer <key>" \
  "http://localhost:8080/api/quick-add/decks?username=alice"

# 添加闪卡
curl -X POST -H "Authorization: Bearer <key>" \
     -H "Content-Type: application/json" \
     -d '{"username":"alice","deckName":"AI问答","front":"Q?","back":"A!","tags":"ai"}' \
     "http://localhost:8080/api/quick-add/card"
```
