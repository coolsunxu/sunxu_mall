# 苏三商城前端Vue项目

基于Vue 3 + TypeScript + Element Plus构建的现代化商城管理系统前端项目。

## 快速开始

### 安装依赖
```bash
cd e:\sunxu\Java\susan\susan_mall\temp
npm install
```

### 启动开发服务器
```bash
npm run dev
```

### 构建生产版本
```bash
npm run build
```

## 项目结构

```
src/
├── views/           # 页面组件
│   ├── login/       # 登录页面
│   ├── dashboard/   # 首页
│   └── error/       # 错误页面
├── layouts/         # 布局组件
├── router/          # 路由配置
├── styles/          # 全局样式
└── main.ts          # 应用入口
```

## 功能特性

- ✅ 登录认证系统
- ✅ 响应式布局设计
- ✅ 现代化UI设计
- ✅ 基于后端API接口设计

## 技术栈

- **Vue 3** - 渐进式JavaScript框架
- **TypeScript** - JavaScript的超集
- **Element Plus** - Vue 3组件库
- **Vue Router 4** - 官方路由管理器
- **Vite** - 下一代前端构建工具

## 环境变量配置

在项目根目录创建 `.env.local` 文件（不提交到版本控制），配置以下环境变量：

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | API 基础 URL | 空（使用 proxy） | `https://api.example.com` |
| `VITE_WS_BASE_URL` | WebSocket 基础 URL | `ws://localhost:8011` | `wss://api.example.com` |
| `VITE_PASSWORD_PUBLIC_KEY` | RSA 公钥（密码加密） | 内置默认值 | - |
| `VITE_HTTP_DEBUG` | 启用 HTTP 请求日志 | `false` | `true` |

### 开发环境示例

```env
VITE_HTTP_DEBUG=true
```

### 生产环境示例

```env
VITE_API_BASE_URL=https://api.example.com
VITE_WS_BASE_URL=wss://api.example.com
VITE_HTTP_DEBUG=false
```

## 浏览器支持

- Chrome >= 87
- Firefox >= 78
- Safari >= 14
- Edge >= 88