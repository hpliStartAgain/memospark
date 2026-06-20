# MemoSpark 微信小程序

基于 **Taro 3 + React + TypeScript** 构建，复用同一套 Spring Boot 后端（JWT 无状态认证）。

## 项目结构

```
miniprogram/
├── config/               Taro 构建配置
│   ├── index.js          主配置（含 TARO_APP_API_URL 环境变量注入）
│   ├── dev.js            开发环境
│   └── prod.js           生产环境
├── src/
│   ├── app.tsx           应用入口（自动 wx.login）
│   ├── app.config.ts     页面路由 + TabBar 配置
│   ├── app.scss          全局样式
│   ├── types/index.ts    TypeScript 类型（与 Web 端对齐）
│   ├── api/index.ts      API 层（Taro.request + Bearer JWT）
│   ├── store/index.ts    本地存储工具（token、user）
│   └── pages/
│       ├── login/        登录页（微信一键登录 + 账号密码）
│       ├── index/        今日仪表盘
│       ├── review/       闪卡复习
│       ├── targets/      备战目标列表
│       ├── target-detail/ 目标详情 + AI 分析技能
│       └── notebook/     错题笔记本
├── project.config.json   微信开发者工具配置（填写 AppID）
├── package.json
├── tsconfig.json
└── babel.config.js
```

## 快速上手

### 1. 安装依赖

```bash
cd miniprogram
npm install
```

### 2. 配置环境变量

复制 `.env.example` 为 `.env`，填写后端 API 地址：

```
TARO_APP_API_URL=https://your-memospark-domain.com
```

> 开发时指向本地后端：`TARO_APP_API_URL=http://192.168.x.x:8080`（小程序不能用 localhost）

### 3. 配置微信 AppID

编辑 `project.config.json`，将 `"appid"` 改为你的微信小程序 AppID。

在后端 `.env` 中填写：
```
WX_APP_ID=your_appid
WX_APP_SECRET=your_appsecret
JWT_SECRET=your_random_32plus_char_secret
```

### 4. 开发构建（监听模式）

```bash
npm run dev:weapp
```

构建产物输出到 `dist/`，在 **微信开发者工具** 中导入整个 `miniprogram/` 目录即可调试。

### 5. 生产构建

```bash
npm run build:weapp
```

### 6. H5 预览（可选）

```bash
npm run dev:h5
```

## 认证流程

```
小程序启动
  └─ app.tsx: wx.login() 获取 code
       └─ POST /api/auth/wx-login  { code }
            └─ 后端兑换 openid，找/建用户，返回 JWT
                 └─ 存入 Taro.setStorageSync('ms_token')
                      └─ 后续所有请求携带 Authorization: Bearer <token>
```

密码登录备用路径：`POST /api/auth/token  { username, password }` → JWT

## TabBar 图标

当前 `app.config.ts` 的 TabBar 为**纯文字**（无图标），可直接编译运行。微信要求每个声明的 `iconPath` 都指向真实存在的 PNG/JPG，否则构建失败，因此默认不声明图标。

如需图标：把下列 4 组 PNG（正常态 + 选中态）放到 `src/assets/icons/`（推荐 81×81px），再在 `app.config.ts` 的每个 tab 项补回 `iconPath` / `selectedIconPath`：

| 文件名               | 用途       |
| -------------------- | ---------- |
| `home.png` / `home-active.png`       | 今日 |
| `review.png` / `review-active.png`   | 复习 |
| `target.png` / `target-active.png`   | 目标 |
| `notebook.png` / `notebook-active.png` | 笔记 |

## 注意事项

- **域名白名单**：在微信公众平台 → 开发 → 开发设置 → 服务器域名，添加后端域名。
- **HTTP 限制**：小程序线上版不允许 HTTP，后端必须配 HTTPS。开发阶段可在微信开发者工具勾选「不校验合法域名」。
- **CORS**：后端若单独部署（不同域），需配置 Spring 的 `@CrossOrigin` 或全局 CORS 策略。
