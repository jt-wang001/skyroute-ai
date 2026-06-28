# SkyRoute AI Backend

SkyRoute AI 后端基础服务，基于 Java 21、Spring Boot 3、Maven、MyBatis-Plus 和 Knife4j。

## 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.x

## 本地配置

应用通过环境变量读取数据库配置：

| 环境变量 | 默认值 |
| --- | --- |
| `SERVER_PORT` | `8080` |
| `MYSQL_HOST` | `127.0.0.1` |
| `MYSQL_PORT` | `3306` |
| `MYSQL_DATABASE` | `skyroute_ai` |
| `MYSQL_USERNAME` | `root` |
| `MYSQL_PASSWORD` | 空 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` |

PowerShell 示例：

```powershell
$env:MYSQL_USERNAME="skyroute"
$env:MYSQL_PASSWORD="your-password"
mvn spring-boot:run
```

## 启动与访问

```powershell
mvn clean spring-boot:run
```

- 健康检查：<http://localhost:8080/api/health>
- Knife4j：<http://localhost:8080/doc.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

## 包结构

```text
com.skyroute.ai
├── common       # 通用响应、错误码与异常
├── config       # CORS、MyBatis-Plus、OpenAPI 配置
├── controller   # HTTP 接口层
├── dto          # 接口入参对象
├── entity       # 数据库实体
├── mapper       # MyBatis-Plus Mapper
├── service      # 业务服务层
└── vo           # 接口出参对象
```
