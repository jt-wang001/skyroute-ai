# SkyRoute AI Frontend

SkyRoute AI 前端基础框架，使用 Vue 3、TypeScript、Vite、Vue Router、Pinia、Axios 和 Element Plus。

## 启动

```powershell
npm install
npm run dev
```

浏览器访问：

```text
http://localhost:5173
```

开发服务器会将 `/api` 代理到：

```text
http://localhost:8080
```

因此本地开发时需先启动 Spring Boot 后端。

## 构建

```powershell
npm run build
```

## 目录结构

```text
src/
├── api/          # Axios 实例和后端接口
├── data/         # 临时演示数据
├── layouts/      # 登录后应用布局
├── router/       # 路由与登录守卫
├── stores/       # Pinia 状态
├── styles/       # 全局样式
├── types/        # TypeScript 类型
├── utils/        # token/localStorage 工具
└── views/
    ├── auth/     # 登录、注册
    ├── dashboard/
    └── mission/  # 列表、创建、详情
```

## 认证流程

1. 登录调用 `POST /api/auth/login`。
2. token 保存到 `localStorage`。
3. Axios 请求拦截器自动添加 `Authorization: Bearer <token>`。
4. 路由守卫阻止未登录用户访问业务页面。
5. 后端返回 401 时，响应拦截器清除本地认证状态并跳转登录页。

## 当前接口状态

注册、登录和用户资料已连接现有后端接口。任务 CRUD、地图绘制和航线算法接口尚未在后端实现，因此任务相关页面当前使用演示数据和交互占位。
