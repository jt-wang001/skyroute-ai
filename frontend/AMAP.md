# 高德地图配置

项目通过高德地图 JS API 2.0 和官方 `@amap/amap-jsapi-loader` 加载地图。

## 1. 创建环境变量

在项目根目录创建 `.env.local`：

```dotenv
VITE_AMAP_KEY=你的高德地图JavaScript API Key
VITE_AMAP_SECURITY_CODE=你的安全密钥
```

`VITE_AMAP_KEY` 必填。`VITE_AMAP_SECURITY_CODE` 是否需要取决于高德控制台中的应用安全配置；新建应用通常建议同时配置。

`.env.local` 已被 `.gitignore` 忽略，不要把真实 Key 提交到 Git。

修改环境变量后，需要重新启动 Vite：

```powershell
npm run dev
```

## 2. 使用组件

在页面中导入：

```vue
<script setup lang="ts">
import MapView from '@/components/MapView.vue'
</script>

<template>
  <MapView />
</template>
```

地图默认中心为上海海洋大学临港校区附近。点击地图后，浏览器开发者工具 Console 会输出：

```text
地图点击坐标：{ longitude: 121.x, latitude: 30.x }
```

## 3. 申请 Key

在高德开放平台控制台创建 Web 端（JS API）应用并获取 Key：

<https://console.amap.com/dev/key/app>
