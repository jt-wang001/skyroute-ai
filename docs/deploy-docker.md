# SkyRoute AI Docker 部署说明

当前部署目标：阿里云轻量应用服务器 Ubuntu 24.04，2 vCPU / 2 GiB。

## 1. 服务器准备

服务器已安装 Docker 后，确认：

```bash
docker -v
sudo docker compose version
sudo docker ps
```

## 2. 上传项目

需要上传整个 `skyroute-ai` 目录到服务器，例如放到：

```bash
/home/admin/skyroute-ai
```

如果本地 SSH/SCP 暂时不可用，可以使用阿里云 Workbench 左侧文件上传功能，上传压缩包后在服务器解压。

推荐服务器目录：

```bash
mkdir -p /home/admin/apps
cd /home/admin/apps
```

## 3. 配置环境变量

进入 docker 目录：

```bash
cd /home/admin/apps/skyroute-ai/docker
cp .env.example .env
nano .env
```

至少修改：

```env
MYSQL_ROOT_PASSWORD=换成你的强密码
JWT_SECRET=换成一串足够长的随机字符串
CORS_ALLOWED_ORIGINS=http://8.219.2.95
```

2GB 内存服务器建议保留：

```env
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
```

## 4. 启动服务

```bash
cd /home/admin/apps/skyroute-ai/docker
sudo docker compose up -d --build
```

查看容器：

```bash
sudo docker compose ps
```

查看日志：

```bash
sudo docker compose logs -f nginx
sudo docker compose logs -f backend
sudo docker compose logs -f algorithm
sudo docker compose logs -f mysql
```

## 5. 访问

浏览器访问：

```text
http://8.219.2.95
```

后端健康检查：

```text
http://8.219.2.95/api/health
```

## 6. 常用命令

重启：

```bash
sudo docker compose restart
```

停止：

```bash
sudo docker compose down
```

停止并删除数据库数据，谨慎使用：

```bash
sudo docker compose down -v
```

重新构建：

```bash
sudo docker compose up -d --build
```

## 7. 端口说明

公网只需要开放：

```text
80   HTTP
443  HTTPS 后续配置
22   SSH
```

`8080`、`8000`、`3306`、`6379` 不需要公网开放，Docker 内部网络会互通。

## 8. 当前架构

```text
Browser
  -> Nginx :80
      -> Vue dist
      -> /api -> backend:8080
backend
  -> mysql:3306
  -> redis:6379
  -> algorithm:8000
```