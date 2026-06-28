<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import type { GeoJsonPolygon, MissionWaypoint } from '@/types/mission'

const props = defineProps<{
  areaGeojson: GeoJsonPolygon | null
  waypoints: MissionWaypoint[]
}>()

const mapContainer = ref<HTMLDivElement>()
const loading = ref(true)
const errorMessage = ref('')

const DEFAULT_CENTER: [number, number] = [121.9025, 30.8967]

let AMapInstance: any = null
let map: any = null
let polygon: any = null
let polyline: any = null
let waypointMarkers: any[] = []
let infoWindow: any = null

function getAreaPath(): [number, number][] {
  const ring = props.areaGeojson?.coordinates?.[0] || []
  if (ring.length <= 1) return []
  return ring.slice(0, -1).map(([longitude, latitude]) => [Number(longitude), Number(latitude)])
}

function getRoutePath(): [number, number][] {
  return [...(props.waypoints || [])]
    .sort((a, b) => a.sequenceNo - b.sequenceNo)
    .map((waypoint) => [Number(waypoint.longitude), Number(waypoint.latitude)])
}

function clearRouteLayers() {
  if (!map) return

  const overlays = [polygon, polyline, ...waypointMarkers].filter(Boolean)
  if (overlays.length > 0) map.remove(overlays)

  polygon = null
  polyline = null
  waypointMarkers = []
  infoWindow?.close()
}

function createWaypointMarker(waypoint: MissionWaypoint) {
  const marker = new AMapInstance.Marker({
    position: [Number(waypoint.longitude), Number(waypoint.latitude)],
    anchor: 'center',
    offset: new AMapInstance.Pixel(-13, -13),
    content: `<div class="route-waypoint-marker">${waypoint.sequenceNo}</div>`,
    zIndex: 120,
  })

  marker.on('click', () => {
    const content = `
      <div class="route-info-window">
        <strong>航点 #${waypoint.sequenceNo}</strong>
        <p>经度：${Number(waypoint.longitude).toFixed(6)}</p>
        <p>纬度：${Number(waypoint.latitude).toFixed(6)}</p>
        <p>高度：${waypoint.altitude} m</p>
        <p>动作：${waypoint.actionType || '-'}</p>
      </div>
    `

    infoWindow.setContent(content)
    infoWindow.open(map, [Number(waypoint.longitude), Number(waypoint.latitude)])
  })

  return marker
}

function renderRoute() {
  if (!map || !AMapInstance) return

  clearRouteLayers()

  const overlays: any[] = []
  const areaPath = getAreaPath()
  const routePath = getRoutePath()

  if (areaPath.length >= 3) {
    polygon = new AMapInstance.Polygon({
      path: areaPath,
      strokeColor: '#36d6d0',
      strokeWeight: 3,
      strokeOpacity: 1,
      fillColor: '#36d6d0',
      fillOpacity: 0.16,
      zIndex: 40,
    })
    map.add(polygon)
    overlays.push(polygon)
  }

  if (routePath.length >= 2) {
    polyline = new AMapInstance.Polyline({
      path: routePath,
      strokeColor: '#ff9c57',
      strokeWeight: 5,
      strokeOpacity: 0.95,
      lineJoin: 'round',
      lineCap: 'round',
      showDir: true,
      zIndex: 80,
    })
    map.add(polyline)
    overlays.push(polyline)
  }

  waypointMarkers = [...(props.waypoints || [])]
    .sort((a, b) => a.sequenceNo - b.sequenceNo)
    .map(createWaypointMarker)

  if (waypointMarkers.length > 0) {
    map.add(waypointMarkers)
    overlays.push(...waypointMarkers)
  }

  if (overlays.length > 0) {
    map.setFitView(overlays, false, [70, 70, 70, 70], 18)
  }
}

async function initializeMap() {
  const key = import.meta.env.VITE_AMAP_KEY?.trim()
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE?.trim()

  if (!key) {
    errorMessage.value = '未配置 VITE_AMAP_KEY，请在 .env.local 中配置高德地图 Key。'
    loading.value = false
    return
  }

  if (securityCode) {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: securityCode }
  }

  try {
    AMapInstance = await AMapLoader.load({
      key,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar'],
    })

    if (!mapContainer.value) return

    map = new AMapInstance.Map(mapContainer.value, {
      center: DEFAULT_CENTER,
      zoom: 16,
      viewMode: '2D',
      dragEnable: true,
      zoomEnable: true,
      scrollWheel: true,
    })

    map.addControl(new AMapInstance.Scale())
    map.addControl(new AMapInstance.ToolBar({ position: 'RB' }))
    infoWindow = new AMapInstance.InfoWindow({ offset: new AMapInstance.Pixel(0, -18) })

    renderRoute()
  } catch (error) {
    console.error('高德地图加载失败：', error)
    errorMessage.value = '地图加载失败，请检查高德 Key、安全密钥和网络配置。'
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.areaGeojson, props.waypoints],
  () => renderRoute(),
  { deep: true },
)

onMounted(initializeMap)

onBeforeUnmount(() => {
  clearRouteLayers()
  map?.destroy()
  map = null
})
</script>

<template>
  <div class="mission-route-map">
    <div ref="mapContainer" class="map-container" />

    <div class="map-legend">
      <span><i class="legend-area" />巡检区域</span>
      <span><i class="legend-route" />规划航线</span>
      <span><i class="legend-point" />航点编号</span>
    </div>

    <div v-if="loading" class="map-state">
      <span class="map-loader" />
      <p>正在加载任务航线地图...</p>
    </div>

    <div v-else-if="errorMessage" class="map-state map-error">
      <strong>地图暂不可用</strong>
      <p>{{ errorMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.mission-route-map {
  position: relative;
  width: 100%;
  min-height: 560px;
  overflow: hidden;
  border: 1px solid rgba(54, 214, 208, 0.2);
  border-radius: 14px;
  background: #071a29;
}

.map-container {
  width: 100%;
  height: 560px;
}

.map-legend {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid rgba(54, 214, 208, 0.28);
  border-radius: 8px;
  color: #dce9f5;
  background: rgba(5, 21, 34, 0.9);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(12px);
  font-size: 12px;
}

.map-legend span {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.map-legend i {
  display: inline-block;
  width: 18px;
  height: 4px;
  border-radius: 99px;
}

.legend-area {
  background: #36d6d0;
  opacity: 0.75;
}

.legend-route {
  background: #ff9c57;
}

.legend-point {
  width: 10px !important;
  height: 10px !important;
  background: #0b2034;
  border: 2px solid #ff9c57;
}

.map-state {
  position: absolute;
  inset: 0;
  z-index: 20;
  display: grid;
  place-content: center;
  color: #8096aa;
  text-align: center;
  background: rgba(7, 26, 41, 0.94);
}

.map-state p {
  max-width: 420px;
  margin: 12px 24px 0;
}

.map-state strong {
  color: #e7f2fb;
}

.map-loader {
  width: 34px;
  height: 34px;
  margin: auto;
  border: 2px solid rgba(54, 214, 208, 0.2);
  border-top-color: #36d6d0;
  border-radius: 50%;
  animation: map-loading 0.8s linear infinite;
}

.map-error {
  color: #ff9c57;
}

@keyframes map-loading {
  to {
    transform: rotate(360deg);
  }
}

:global(.route-waypoint-marker) {
  display: grid;
  width: 26px;
  height: 26px;
  place-items: center;
  border: 2px solid #ff9c57;
  border-radius: 50%;
  color: #061421;
  background: #ffd6a8;
  box-shadow: 0 0 16px rgba(255, 156, 87, 0.85);
  font: 700 12px/1 Arial, sans-serif;
}

:global(.route-info-window) {
  min-width: 180px;
  color: #1f2d3d;
  font-size: 13px;
}

:global(.route-info-window strong) {
  display: block;
  margin-bottom: 8px;
  color: #0b2034;
  font-size: 14px;
}

:global(.route-info-window p) {
  margin: 4px 0;
}

@media (max-width: 760px) {
  .map-legend {
    right: 12px;
    left: 12px;
    flex-wrap: wrap;
  }
}
</style>
