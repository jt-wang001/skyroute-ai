# SkyRoute AI

SkyRoute AI 是一个低空无人机任务规划平台。用户可以在地图上绘制巡检区域，填写飞行高度、速度、航向重叠率、旁向重叠率等参数，系统自动生成割草机式航线，并支持任务保存、航点展示、风险评估和 GeoJSON 导出。

项目采用前后端分离和独立算法服务架构：

```text
Vue3 前端
  -> Spring Boot 后端
      -> MySQL
      -> FastAPI 算法服务
  -> Nginx / Docker 部署
```

## 项目特性

- 用户注册、登录和 JWT 鉴权
- 高德地图区域绘制
- 省 / 市 / 区县选择与地址定位
- 巡检区域 GeoJSON Polygon 生成
- FastAPI 算法服务生成矩形区域割草机式航线
- 航点保存与地图展示
- 任务列表、任务详情、任务删除
- 任务详情地图展示 Polygon、Polyline、Marker
- 航点点击展示经纬度、高度和序号
- 任务 GeoJSON 导出
- 风险评估：高度、速度、预计时间、航点数
- Docker Compose 一键部署

## 技术栈

### Frontend

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Axios
- Element Plus
- 高德地图 JS API

### Backend

- Java 21
- Spring Boot 3
- Maven
- MyBatis Plus
- JWT
- BCrypt
- Knife4j / SpringDoc

### Algorithm

- Python 3.10+
- FastAPI
- Pydantic
- Uvicorn

### Infrastructure

- MySQL 8
- Redis 7
- Docker
- Docker Compose
- Nginx

## 项目结构

```text
skyroute-ai/
├─ backend/        Spring Boot 后端服务
├─ frontend/       Vue3 前端源码
├─ algorithm/      Python FastAPI 算法服务
├─ docker/         Docker Compose、Nginx、MySQL 初始化配置
├─ docs/           数据库、部署、项目讲解文档
└─ README.md       项目首页说明
```

## 核心流程

### 创建任务流程

```text
1. 用户在前端地图绘制巡检区域
2. 前端生成 GeoJSON Polygon
3. 用户填写飞行参数
4. 前端调用 POST /api/missions
5. 后端校验 JWT 并保存任务区域
6. 后端调用 FastAPI /generate-route
7. 算法服务返回航点、航程、预计时间
8. 后端保存航点数据
9. 前端进入任务详情页展示航线
```

### 登录鉴权流程

```text
1. 用户登录
2. 后端校验 BCrypt 密码
3. 后端签发 JWT
4. 前端保存 token
5. Axios 后续请求自动携带 Authorization: Bearer <token>
6. 后端过滤器校验 token 并识别当前用户
```

## 数据库设计

主要数据表：

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

数据库 SQL 位于：

```text
backend/src/main/resources/db/schema-v1.sql
docker/mysql/init/01-schema.sql
```

## 本地开发

### 1. 后端

进入后端目录：

```bash
cd backend
```

启动：

```bash
mvn spring-boot:run
```

默认配置：

```text
Backend: http://localhost:8080
Health:  http://localhost:8080/api/health
```

### 2. 前端

进入前端目录：

```bash
cd frontend
```

安装依赖：

```bash
npm install
```

创建本地环境变量文件：

```text
frontend/.env.local
```

示例：

```env
VITE_API_BASE_URL=http://localhost:8080/api
VITE_AMAP_KEY=your-amap-key
VITE_AMAP_SECURITY_CODE=your-amap-security-code
```

启动：

```bash
npm run dev
```

默认访问：

```text
http://localhost:5173
```

### 3. 算法服务

进入算法目录：

```bash
cd algorithm
```

创建虚拟环境并安装依赖：

```bash
python -m venv .venv
```

Windows:

```bash
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Linux / macOS:

```bash
source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

默认访问：

```text
http://localhost:8000/health
```

## Docker 部署

部署配置位于：

```text
docker/
```

启动：

```bash
cd docker
cp .env.example .env
docker compose up -d --build
```

查看状态：

```bash
docker compose ps
```

查看日志：

```bash
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f algorithm
```

详细部署说明：

```text
docs/deploy-docker.md
```

## 接口示例

### 健康检查

```http
GET /api/health
```

### 注册

```http
POST /api/auth/register
```

### 登录

```http
POST /api/auth/login
```

### 创建任务

```http
POST /api/missions
Authorization: Bearer <token>
```

### 任务详情

```http
GET /api/missions/{id}
Authorization: Bearer <token>
```

### 导出 GeoJSON

```http
GET /api/missions/{id}/export/geojson
Authorization: Bearer <token>
```

## 风险评估规则

| 条件 | 风险等级 |
|---|---|
| 飞行高度 > 120 米 | HIGH |
| 飞行高度过低 | MEDIUM |
| 飞行速度 > 15 m/s | MEDIUM |
| 预计时间 > 25 分钟 | MEDIUM |
| 航点数 > 200 | HIGH |

返回示例：

```json
{
  "riskLevel": "HIGH",
  "riskMessages": [
    "航点数超过 200 个，航线复杂度较高"
  ]
}
```

## 文档

- [项目结构讲解](docs/project-walkthrough.md)
- [数据库设计](docs/database-v1.md)
- [Docker 部署说明](docs/deploy-docker.md)
- [启动说明](docs/STARTUP.md)

## 安全说明

仓库不会提交真实环境变量和密钥。

以下文件应保留在本地或服务器，不应提交：

```text
.env
.env.local
.env.production
docker/.env
certbot/
node_modules/
target/
.venv/
```

如果需要配置高德地图 Key，请在本地创建：

```text
frontend/.env.local
```

## Roadmap

- KML 导出
- AI 自然语言参数解析
- Redis 接入 token 黑名单 / 任务缓存
- 天气风险评估
- 禁飞区风险评估
- 电量与续航评估
- 航线算法优化
- HTTPS 与域名备案后正式上线

## License

No license yet.