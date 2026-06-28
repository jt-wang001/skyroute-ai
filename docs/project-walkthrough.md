# SkyRoute AI 项目结构讲解

这份文档的目标不是讲所有代码细节，而是帮你先建立项目地图：每个目录干什么、一次请求怎么流转、以后想改功能应该去哪里改。

## 1. 项目整体分层

SkyRoute AI 可以先理解成 5 个部分：

```text
用户页面        frontend
业务接口        backend
航线算法        algorithm
数据存储        MySQL / Redis
上线入口        Docker / Nginx
```

整体访问链路：

```text
浏览器
  -> Nginx
      -> Vue 前端静态页面
      -> /api 转发到 Spring Boot 后端
  -> Spring Boot 后端
      -> MySQL 保存数据
      -> FastAPI 生成航线
```

线上部署时，用户访问：

```text
http://106.14.125.162
```

Nginx 接收请求：

- 访问页面：返回 Vue 打包后的静态文件；
- 访问 `/api/**`：转发给 Spring Boot 后端；
- 后端需要生成航线时：调用 Python FastAPI 算法服务；
- 后端需要保存数据时：写入 MySQL。

## 2. 根目录说明

当前主项目目录：

```text
D:\JavaProject\skyroute-ai
```

目录作用：

```text
skyroute-ai
├─ backend      Spring Boot 后端
├─ frontend     前端目录，目前主仓库里只有说明文件
├─ algorithm    Python FastAPI 算法服务
├─ docker       Docker / Nginx / MySQL 部署配置
├─ docs         项目文档
└─ README.md    项目总说明
```

注意：当前实际前端源码在：

```text
D:\VSCodeProject\skyroute-ai-frontend
```

上线时，前端会先执行 `npm run build`，然后把生成的 `dist` 放进：

```text
D:\JavaProject\skyroute-ai\docker\frontend\dist
```

再由 Nginx 托管。

## 3. 后端 backend 怎么看

后端位置：

```text
backend/src/main/java/com/skyroute/ai
```

核心目录：

```text
controller   接口入口，接收前端请求
service      业务逻辑
mapper       数据库操作
entity       数据库表对应的 Java 类
dto          前端传给后端的数据
vo           后端返回给前端的数据
common       通用类，比如 Result、JWT、风险等级
config       配置类，比如跨域、JWT 拦截器、接口文档
client       调用 Python 算法服务
```

你可以先重点看这些文件：

```text
AuthController.java          登录、注册接口
UserController.java          当前用户信息接口
MissionController.java       任务接口
AuthServiceImpl.java         登录、注册逻辑
MissionServiceImpl.java      创建任务、查询任务、删除任务、导出 GeoJSON
AlgorithmClient.java         调用 Python FastAPI 算法服务
JwtAuthenticationFilter.java JWT 校验
RiskAssessmentServiceImpl.java 风险评估规则
```

后端接口大致是：

```text
POST   /api/auth/register              注册
POST   /api/auth/login                 登录
GET    /api/user/profile               获取当前用户
GET    /api/health                     健康检查
POST   /api/missions                   创建任务
GET    /api/missions                   任务列表
GET    /api/missions/{id}              任务详情
DELETE /api/missions/{id}              删除任务
GET    /api/missions/{id}/export/geojson 导出 GeoJSON
```

## 4. 前端 frontend 怎么看

实际前端源码位置：

```text
D:\VSCodeProject\skyroute-ai-frontend
```

核心目录：

```text
src/views       页面
src/components  公共组件，比如地图组件
src/api         调后端接口
src/router      页面路由
src/stores      Pinia 状态
src/types       TypeScript 类型
src/utils       工具函数
```

核心页面：

```text
LoginView.vue             登录页
RegisterView.vue          注册页
DashboardView.vue         首页
MissionListView.vue       任务列表
MissionCreateView.vue     创建任务
MissionDetailView.vue     任务详情
```

地图相关组件：

```text
MapView.vue          创建任务时画巡检区域
MissionRouteMap.vue  任务详情中展示区域、航线、航点
```

接口调用位置：

```text
src/api/http.ts      Axios 基础配置，自动携带 token
src/api/auth.ts      登录注册接口
src/api/mission.ts   任务接口
```

前端登录后的 token 保存逻辑在：

```text
src/utils/storage.ts
```

## 5. 算法服务 algorithm 怎么看

算法服务位置：

```text
algorithm/main.py
```

它是一个 Python FastAPI 服务，主要接口：

```text
GET  /health
POST /generate-route
```

后端创建任务时，会把巡检区域和飞行参数发给：

```text
POST http://algorithm:8000/generate-route
```

算法服务返回：

```text
distanceMeters
estimatedTimeSeconds
waypointCount
waypoints
```

后端再把这些航点保存到 MySQL 的 `waypoint` 表。

## 6. 数据库怎么理解

当前主要 4 张表：

```text
user          用户表
mission       飞行任务表
mission_area  巡检区域表
waypoint      航点表
```

关系：

```text
user 1 ---- N mission
mission 1 ---- 1 mission_area
mission 1 ---- N waypoint
```

也就是：

- 一个用户可以创建多个任务；
- 一个任务有一个巡检区域；
- 一个任务有多个航点。

常用查看命令：

```bash
docker exec -it skyroute-mysql mysql -uroot -p
```

进入 MySQL 后：

```sql
USE skyroute_ai;
SHOW TABLES;
SELECT * FROM `user`;
SELECT * FROM mission;
SELECT * FROM mission_area;
SELECT * FROM waypoint LIMIT 20;
```

## 7. 创建任务完整流程

这是项目最核心的一条链路。

```text
1. 用户进入创建任务页
2. 前端加载高德地图
3. 用户点击地图，绘制巡检区域 Polygon
4. 用户填写飞行高度、速度、重叠率
5. 前端调用 POST /api/missions
6. 后端 JWT 拦截器校验登录状态
7. MissionController 接收请求
8. MissionServiceImpl 创建 mission 和 mission_area
9. 后端调用 AlgorithmClient
10. AlgorithmClient 请求 Python /generate-route
11. Python 生成割草机式航线
12. 后端保存 waypoint
13. 后端计算总航程、预计时间、航点数
14. 前端跳转任务详情页
15. 任务详情页调用 GET /api/missions/{id}
16. 前端地图展示区域 Polygon、航线 Polyline、航点 Marker
```

如果创建任务失败，优先看：

```text
docker compose logs -f backend
docker compose logs -f algorithm
```

## 8. 登录 JWT 完整流程

注册：

```text
1. 前端 POST /api/auth/register
2. 后端检查用户名是否重复
3. BCrypt 加密密码
4. 保存 user 表
```

登录：

```text
1. 前端 POST /api/auth/login
2. 后端查 user 表
3. BCrypt 校验密码
4. 生成 JWT token
5. 前端保存 token
```

访问需要登录的接口：

```text
1. 前端 Axios 自动加 Authorization: Bearer token
2. 后端 JwtAuthenticationFilter 校验 token
3. 校验成功后，把 userId 放到 UserContext
4. Controller / Service 从 UserContext 获取当前用户
```

为什么可以做到“用户只能看自己的任务”：

后端查询任务时会同时带上：

```text
mission.id
currentUserId
```

如果任务不是当前用户的，就查不到。

## 9. 风险评估模块怎么理解

风险评估在：

```text
RiskAssessmentServiceImpl.java
```

当前风险等级：

```text
LOW
MEDIUM
HIGH
```

当前规则：

```text
高度 > 120 米       HIGH
高度 < 30 米        MEDIUM
速度 > 15 m/s       MEDIUM
预计时间 > 25 分钟  MEDIUM
航点数 > 200        HIGH
```

任务详情接口会返回：

```json
"riskAssessment": {
  "riskLevel": "HIGH",
  "riskMessages": ["航点数超过 200 个，航线复杂度较高"]
}
```

前端任务详情页会把它展示成标签和原因列表。

后续要加天气、禁飞区、电量，优先扩展这个服务。

## 10. Docker 和部署怎么理解

部署目录：

```text
docker
├─ docker-compose.yml
├─ nginx/nginx.conf
├─ mysql/init/01-schema.sql
├─ mysql/conf.d/my.cnf
└─ frontend/dist
```

线上服务：

```text
skyroute-nginx      对外入口，监听 80
skyroute-backend    Spring Boot 后端
skyroute-algorithm  Python FastAPI 算法服务
skyroute-mysql      MySQL
skyroute-redis      Redis，目前预留
```

线上常用命令：

```bash
cd /root/apps/skyroute-ai/docker
docker compose ps
docker compose logs -f backend
docker compose logs -f nginx
docker compose restart
docker compose up -d --build
```

如果服务器重启，容器通常会自动启动，因为配置了：

```yaml
restart: unless-stopped
```

如果没起来，手动执行：

```bash
cd /root/apps/skyroute-ai/docker
docker compose up -d
```

## 11. 以后想改功能，应该改哪里

### 改登录 / 注册

后端：

```text
AuthController.java
AuthServiceImpl.java
JwtUtil.java
JwtAuthenticationFilter.java
```

前端：

```text
LoginView.vue
RegisterView.vue
src/api/auth.ts
```

### 改创建任务

后端：

```text
MissionController.java
MissionServiceImpl.java
MissionCreateDTO.java
```

前端：

```text
MissionCreateView.vue
MapView.vue
src/api/mission.ts
```

算法：

```text
algorithm/main.py
```

### 改任务详情地图

前端：

```text
MissionDetailView.vue
MissionRouteMap.vue
```

后端：

```text
MissionServiceImpl.java
MissionDetailVO.java
```

### 改数据库字段

需要同步改：

```text
MySQL 表结构
Entity
DTO / VO
Service
前端 TypeScript 类型
```

### 改上线部署

主要看：

```text
docker/docker-compose.yml
docker/nginx/nginx.conf
backend/Dockerfile
algorithm/Dockerfile
docs/deploy-docker.md
```

## 12. 推荐学习顺序

不要从所有文件一起看。按这个顺序：

```text
1. 看 MissionCreateView.vue，理解前端怎么提交任务
2. 看 src/api/mission.ts，理解调用了哪个接口
3. 看 MissionController.java，理解后端入口
4. 看 MissionServiceImpl.java，理解任务如何保存
5. 看 AlgorithmClient.java，理解后端怎么调用 Python
6. 看 algorithm/main.py，理解航线怎么生成
7. 看 MissionDetailView.vue 和 MissionRouteMap.vue，理解地图怎么展示
```

每次只追一条链路，不要同时看所有模块。

## 13. 当前最重要的理解

你只需要先记住这句话：

```text
前端负责展示和收集参数，后端负责业务和数据，算法服务负责生成航线，数据库负责保存结果，Nginx/Docker负责上线运行。
```

等这句话真正理解了，再去看代码会清楚很多。

