# Algorithm

SkyRoute AI 航线规划算法服务，基于 Python 3.10+、FastAPI 和 Pydantic。

## 当前接口

- `GET /health`：服务健康检查。
- `POST /generate-route`：根据巡检区域和飞行参数生成模拟航点。
- `GET /docs`：FastAPI 自动生成的 Swagger API 文档。

## 安装依赖

在 `algorithm` 目录执行：

```bash
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
```

## 启动服务

```bash
uvicorn main:app --reload --port 8000
```

启动后访问：

- 健康检查：<http://127.0.0.1:8000/health>
- API 文档：<http://127.0.0.1:8000/docs>

## 请求示例

```json
{
  "areaGeojson": {
    "type": "Polygon",
    "coordinates": [
      [
        [121.9101, 30.8891],
        [121.9121, 30.8891],
        [121.9121, 30.8911],
        [121.9101, 30.8911],
        [121.9101, 30.8891]
      ]
    ]
  },
  "flightHeight": 80,
  "flightSpeed": 8,
  "sideOverlap": 65
}
```
