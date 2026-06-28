# 本地启动说明

## 当前状态

项目目前只有基础目录和文档，没有可执行代码。以下命令是后续生成对应框架后的统一启动约定。

## 环境要求

| 模块 | 推荐环境 |
| --- | --- |
| Backend | JDK 21、Maven 3.9+ |
| Frontend | Node.js 20+、npm |
| Algorithm | Python 3.11+、pip |
| Container | Docker Desktop |

## Backend

进入 `backend` 目录后运行 Maven Wrapper：

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS：

```bash
./mvnw spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

## Algorithm

Windows PowerShell：

```powershell
cd algorithm
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --reload
```

Linux/macOS：

```bash
cd algorithm
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload
```

## Docker

后续加入 `compose.yaml` 后，计划通过以下命令统一启动：

```bash
docker compose up --build
```

端口、环境变量和服务依赖将在框架初始化后确定。

