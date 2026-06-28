# SkyRoute AI

SkyRoute AI 是一个低空无人机任务规划平台。用户将在地图上绘制巡检区域，配置飞行高度、速度、航线重叠率等参数，由系统自动生成“割草机式”航线，并支持任务保存、航点展示以及 GeoJSON/KML 导出。后续可扩展 AI 自然语言参数解析能力。

## 当前状态

当前仓库仅完成项目目录初始化和技术方案说明，尚未生成框架代码，也不包含任何业务实现。

## 计划技术栈

- 后端：Java 21、Spring Boot 3、Maven
- 前端：Vue 3、TypeScript、Vite
- 算法服务：Python、FastAPI
- 部署：Docker、Docker Compose（后续引入）

## 目录结构

```text
skyroute-ai/
├── backend/        # Spring Boot 后端服务
├── frontend/       # Vue 3 Web 前端
├── algorithm/      # Python 航线规划算法服务
├── docs/           # 架构、接口和开发文档
├── docker/         # 容器化与部署配置
└── README.md       # 项目总览
```

## 各目录作用

### `backend`

计划承载任务管理、参数校验、用户数据、航点数据、文件导出和前后端 API。后续由 Spring Boot 3 + Maven + Java 21 实现，并负责调用独立的算法服务。

### `frontend`

计划承载地图绘制、任务参数表单、航线和航点可视化、任务管理及导出操作。后续使用 Vue 3 + TypeScript + Vite 实现。

### `algorithm`

计划承载巡检区域几何处理、割草机式航线生成、航点计算及航线优化。后续使用 Python FastAPI 提供独立 HTTP API，便于算法快速迭代。

### `docs`

存放总体架构、数据模型、接口协议、开发规范、部署说明和设计决策等项目文档。

### `docker`

存放后端、前端、算法服务的 Dockerfile，以及 Docker Compose、反向代理和环境配置模板等部署文件。

## 环境准备

正式生成各模块框架后，开发环境预计需要：

- JDK 21
- Maven 3.9+
- Node.js 20+ 与 npm
- Python 3.11+
- Docker Desktop（可选）

## 启动说明

当前只有目录和文档，暂无可启动程序。各框架创建完成后的预期启动方式如下。

### 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 启动算法服务

```bash
cd algorithm
python -m venv .venv
```

Linux/macOS：

```bash
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Windows PowerShell：

```powershell
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

更完整的启动约定见 [`docs/STARTUP.md`](docs/STARTUP.md)。

