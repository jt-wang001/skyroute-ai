<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

export interface MapCoordinate {
  longitude: number
  latitude: number
}

export interface GeoJsonPolygon {
  type: 'Polygon'
  coordinates: [number, number][][]
}

interface DistrictOption {
  name: string
  adcode?: string
  center?: [number, number]
}

const emit = defineEmits<{
  coordinateChange: [coordinate: MapCoordinate]
  areaChange: [geoJson: GeoJsonPolygon | null]
}>()

const mapContainer = ref<HTMLDivElement>()
const loading = ref(true)
const locating = ref(false)
const errorMessage = ref('')
const locationMessage = ref('')
const selectedCoordinate = ref<MapCoordinate | null>(null)
const vertices = ref<MapCoordinate[]>([])

const provinces = ref<DistrictOption[]>([])
const cities = ref<DistrictOption[]>([])
const districts = ref<DistrictOption[]>([])
const selectedProvinceName = ref('上海市')
const selectedCityName = ref('上海市')
const selectedDistrictName = ref('浦东新区')
const addressKeyword = ref('上海海洋大学')

let AMapApi: any = null
let map: any = null
let vertexMarkers: any[] = []
let polyline: any = null
let polygon: any = null
let searchMarker: any = null
let geocoder: any = null

const SHANGHAI_OCEAN_UNIVERSITY: [number, number] = [121.9025, 30.8967]

const drawingHint = computed(() => {
  if (vertices.value.length === 0) return '点击地图添加第一个顶点'
  if (vertices.value.length < 3) {
    return `已添加 ${vertices.value.length} 个顶点，至少还需 ${3 - vertices.value.length} 个`
  }
  return `区域已闭合，共 ${vertices.value.length} 个顶点`
})

function toPath(): [number, number][] {
  return vertices.value.map(({ longitude, latitude }) => [longitude, latitude])
}

function createGeoJsonPolygon(): GeoJsonPolygon | null {
  if (vertices.value.length < 3) return null
  const ring = toPath()
  const firstCoordinate: [number, number] = [...ring[0]]

  return {
    type: 'Polygon',
    coordinates: [[...ring, firstCoordinate]],
  }
}

function getLngLatTuple(center: any): [number, number] | undefined {
  if (!center) return undefined
  if (Array.isArray(center) && center.length >= 2) return [Number(center[0]), Number(center[1])]
  if (typeof center.getLng === 'function' && typeof center.getLat === 'function') {
    return [Number(center.getLng()), Number(center.getLat())]
  }
  if ('lng' in center && 'lat' in center) return [Number(center.lng), Number(center.lat)]
  return undefined
}

function toDistrictOption(district: any): DistrictOption {
  return {
    name: district.name,
    adcode: district.adcode,
    center: getLngLatTuple(district.center),
  }
}

function moveTo(center: [number, number], zoom = 16) {
  if (!map) return
  map.setZoomAndCenter(zoom, center)
}

function setSearchMarker(center: [number, number], title: string) {
  if (!map || !AMapApi) return

  if (searchMarker) map.remove(searchMarker)
  searchMarker = new AMapApi.Marker({
    position: center,
    anchor: 'bottom-center',
    title,
    zIndex: 120,
  })
  map.add(searchMarker)
}

function searchDistrictChildren(keyword: string): Promise<DistrictOption[]> {
  return new Promise((resolve) => {
    if (!AMapApi || !keyword) {
      resolve([])
      return
    }

    const districtSearch = new AMapApi.DistrictSearch({
      subdistrict: 1,
      extensions: 'base',
    })

    districtSearch.search(keyword, (status: string, result: any) => {
      if (status !== 'complete') {
        resolve([])
        return
      }

      const root = result?.districtList?.[0]
      const children = root?.districtList || []
      resolve(children.map(toDistrictOption))
    })
  })
}

async function loadProvinces() {
  provinces.value = await searchDistrictChildren('中国')

  if (!provinces.value.some((item) => item.name === selectedProvinceName.value)) {
    selectedProvinceName.value = provinces.value[0]?.name || '上海市'
  }

  await handleProvinceChange(false)
}

async function handleProvinceChange(shouldMove = true) {
  cities.value = []
  districts.value = []
  selectedCityName.value = ''
  selectedDistrictName.value = ''

  const province = provinces.value.find((item) => item.name === selectedProvinceName.value)
  if (shouldMove && province?.center) moveTo(province.center, 8)

  const children = await searchDistrictChildren(selectedProvinceName.value)
  cities.value = children

  if (selectedProvinceName.value === '上海市') {
    selectedCityName.value = '上海市'
  } else {
    selectedCityName.value = children[0]?.name || ''
  }

  await handleCityChange(false)
}

async function handleCityChange(shouldMove = true) {
  districts.value = []
  selectedDistrictName.value = ''

  const city = cities.value.find((item) => item.name === selectedCityName.value)
  if (shouldMove && city?.center) moveTo(city.center, 11)

  const keyword = selectedCityName.value || selectedProvinceName.value
  const children = await searchDistrictChildren(keyword)
  districts.value = children

  if (selectedProvinceName.value === '上海市') {
    selectedDistrictName.value = children.find((item) => item.name === '浦东新区')?.name || children[0]?.name || ''
  } else {
    selectedDistrictName.value = children[0]?.name || ''
  }

  if (selectedDistrictName.value) handleDistrictChange(false)
}

function handleDistrictChange(shouldMove = true) {
  const district = districts.value.find((item) => item.name === selectedDistrictName.value)
  if (shouldMove && district?.center) moveTo(district.center, 13)
}

function locateByAddress() {
  if (!map || !geocoder) return

  const parts = [
    selectedProvinceName.value,
    selectedCityName.value,
    selectedDistrictName.value,
    addressKeyword.value.trim(),
  ].filter(Boolean)

  const keyword = parts.join('')
  if (!keyword) {
    locationMessage.value = '请先选择地区或输入具体位置'
    return
  }

  locating.value = true
  locationMessage.value = '正在定位...'

  geocoder.getLocation(keyword, (status: string, result: any) => {
    locating.value = false

    const geocode = result?.geocodes?.[0]
    const center = getLngLatTuple(geocode?.location)

    if (status !== 'complete' || !center) {
      locationMessage.value = '未找到该位置，请补充更具体的地址'
      return
    }

    moveTo(center, 17)
    setSearchMarker(center, keyword)
    locationMessage.value = `已定位到 ${geocode.formattedAddress || keyword}，请在地图上点击具体巡检点`
  })
}

function clearArea() {
  if (map) {
    if (vertexMarkers.length > 0) map.remove(vertexMarkers)
    if (polyline) map.remove(polyline)
    if (polygon) map.remove(polygon)
  }

  vertexMarkers = []
  polyline = null
  polygon = null
  vertices.value = []
  selectedCoordinate.value = null
  emit('areaChange', null)
}

function handleMapClick(event: { lnglat: any }) {
  if (!map || !AMapApi) return

  const coordinate: MapCoordinate = {
    longitude: Number(event.lnglat.getLng().toFixed(6)),
    latitude: Number(event.lnglat.getLat().toFixed(6)),
  }

  selectedCoordinate.value = coordinate
  vertices.value.push(coordinate)
  const path = toPath()

  const marker = new AMapApi.Marker({
    position: [coordinate.longitude, coordinate.latitude],
    anchor: 'bottom-center',
    title: `顶点 ${vertices.value.length}`,
  })
  vertexMarkers.push(marker)
  map.add(marker)

  if (polyline) {
    polyline.setPath(path)
  } else {
    polyline = new AMapApi.Polyline({
      path,
      strokeColor: '#36d6d0',
      strokeWeight: 4,
      strokeOpacity: 0.95,
      lineJoin: 'round',
      lineCap: 'round',
      zIndex: 50,
    })
    map.add(polyline)
  }

  if (vertices.value.length >= 3) {
    if (polygon) {
      polygon.setPath(path)
    } else {
      polygon = new AMapApi.Polygon({
        path,
        strokeColor: '#36d6d0',
        strokeWeight: 3,
        strokeOpacity: 1,
        fillColor: '#36d6d0',
        fillOpacity: 0.2,
        zIndex: 40,
      })
      map.add(polygon)
    }
  }

  const geoJson = createGeoJsonPolygon()
  console.log('地图点击坐标：', coordinate)
  if (geoJson) console.log('巡检区域 GeoJSON：', geoJson)
  emit('coordinateChange', coordinate)
  emit('areaChange', geoJson)
}

async function initializeMap() {
  const key = import.meta.env.VITE_AMAP_KEY?.trim()
  const securityCode = import.meta.env.VITE_AMAP_SECURITY_CODE?.trim()

  if (!key) {
    errorMessage.value = '未配置 VITE_AMAP_KEY，请在 .env.local 中填写高德地图 Key。'
    loading.value = false
    return
  }

  if (securityCode) {
    ;(window as any)._AMapSecurityConfig = { securityJsCode: securityCode }
  }

  try {
    AMapApi = await AMapLoader.load({
      key,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.DistrictSearch', 'AMap.Geocoder'],
    })

    if (!mapContainer.value) return

    map = new AMapApi.Map(mapContainer.value, {
      center: SHANGHAI_OCEAN_UNIVERSITY,
      zoom: 16,
      viewMode: '2D',
      dragEnable: true,
      zoomEnable: true,
      doubleClickZoom: true,
      scrollWheel: true,
    })

    map.addControl(new AMapApi.Scale())
    map.addControl(new AMapApi.ToolBar({ position: 'RB' }))
    map.on('click', handleMapClick)

    geocoder = new AMapApi.Geocoder({ city: '全国' })
    setSearchMarker(SHANGHAI_OCEAN_UNIVERSITY, '上海海洋大学')
    await loadProvinces()
  } catch (error) {
    console.error('高德地图加载失败：', error)
    errorMessage.value = '高德地图加载失败，请检查 Key、安全密钥和网络配置。'
  } finally {
    loading.value = false
  }
}

onMounted(initializeMap)

onBeforeUnmount(() => {
  clearArea()
  if (searchMarker && map) map.remove(searchMarker)
  map?.destroy()
  map = null
})
</script>

<template>
  <div class="map-view">
    <div ref="mapContainer" class="map-container" />

    <div class="location-toolbar">
      <div class="location-title">
        <small>LOCATION SEARCH</small>
        <strong>先定位，再点击地图精确选择</strong>
      </div>

      <div class="location-fields">
        <select v-model="selectedProvinceName" @change="handleProvinceChange()">
          <option v-for="province in provinces" :key="province.name" :value="province.name">
            {{ province.name }}
          </option>
        </select>

        <select v-model="selectedCityName" @change="handleCityChange()">
          <option v-for="city in cities" :key="city.name" :value="city.name">
            {{ city.name }}
          </option>
        </select>

        <select v-model="selectedDistrictName" @change="handleDistrictChange()">
          <option value="">区/县</option>
          <option v-for="district in districts" :key="district.name" :value="district.name">
            {{ district.name }}
          </option>
        </select>

        <input
          v-model="addressKeyword"
          type="text"
          placeholder="输入具体位置，如学校、园区、道路"
          @keydown.enter.prevent="locateByAddress"
        />

        <button type="button" :disabled="locating" @click.stop="locateByAddress">
          {{ locating ? '定位中' : '定位' }}
        </button>
      </div>

      <p v-if="locationMessage" class="location-message">{{ locationMessage }}</p>
    </div>

    <div class="drawing-toolbar">
      <div>
        <small>AREA DRAWING</small>
        <strong>{{ drawingHint }}</strong>
      </div>
      <button type="button" :disabled="vertices.length === 0" @click.stop="clearArea">清除区域</button>
    </div>

    <div v-if="selectedCoordinate" class="coordinate-panel">
      <span class="coordinate-status" />
      <div>
        <small>LAST VERTEX · {{ vertices.length }}</small>
        <strong>{{ selectedCoordinate.longitude }}, {{ selectedCoordinate.latitude }}</strong>
      </div>
    </div>

    <div v-if="loading" class="map-state">
      <span class="map-loader" />
      <p>正在加载高德地图...</p>
    </div>

    <div v-else-if="errorMessage" class="map-state map-error">
      <strong>地图暂不可用</strong>
      <p>{{ errorMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.map-view {
  position: relative;
  width: 100%;
  min-height: 560px;
  overflow: hidden;
  border: 1px solid rgba(54, 214, 208, 0.2);
  border-radius: 12px;
  background: #071a29;
}

.map-container {
  width: 100%;
  height: 560px;
}

.location-toolbar,
.drawing-toolbar,
.coordinate-panel {
  position: absolute;
  z-index: 10;
  border: 1px solid rgba(54, 214, 208, 0.35);
  border-radius: 8px;
  color: #dce9f5;
  background: rgba(5, 21, 34, 0.92);
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(12px);
}

.location-toolbar {
  top: 16px;
  right: 16px;
  left: 16px;
  padding: 12px;
}

.location-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.location-title small,
.drawing-toolbar small,
.coordinate-panel small {
  display: block;
  margin-bottom: 4px;
  color: #6c899d;
  font: 9px monospace;
  letter-spacing: 0.14em;
}

.location-title strong,
.drawing-toolbar strong,
.coordinate-panel strong {
  display: block;
}

.location-title strong {
  font-size: 13px;
}

.location-fields {
  display: grid;
  grid-template-columns: 120px 120px 120px minmax(180px, 1fr) 76px;
  gap: 8px;
}

.location-fields select,
.location-fields input {
  min-width: 0;
  height: 34px;
  border: 1px solid rgba(54, 214, 208, 0.25);
  border-radius: 6px;
  color: #e7f2fb;
  background: rgba(4, 18, 31, 0.92);
  outline: none;
}

.location-fields select {
  padding: 0 8px;
}

.location-fields input {
  padding: 0 10px;
}

.location-fields button,
.drawing-toolbar button {
  height: 34px;
  border: 1px solid rgba(255, 156, 87, 0.35);
  border-radius: 6px;
  color: #ffb27c;
  background: rgba(255, 156, 87, 0.08);
  cursor: pointer;
}

.location-fields button:disabled,
.drawing-toolbar button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.location-message {
  margin: 8px 0 0;
  color: #9edbd8;
  font-size: 12px;
}

.drawing-toolbar {
  top: 130px;
  left: 18px;
  display: flex;
  gap: 20px;
  align-items: center;
  padding: 12px 13px 12px 15px;
}

.drawing-toolbar strong {
  font-size: 12px;
}

.coordinate-panel {
  bottom: 18px;
  left: 18px;
  display: flex;
  gap: 11px;
  align-items: center;
  padding: 12px 15px;
}

.coordinate-panel strong {
  color: #36d6d0;
  font: 13px monospace;
}

.coordinate-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff9c57;
  box-shadow: 0 0 11px #ff9c57;
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

@media (max-width: 920px) {
  .location-fields {
    grid-template-columns: repeat(3, 1fr);
  }

  .location-fields input {
    grid-column: span 2;
  }
}

@media (max-width: 640px) {
  .location-toolbar {
    right: 12px;
    left: 12px;
  }

  .location-fields {
    grid-template-columns: 1fr;
  }

  .location-fields input {
    grid-column: auto;
  }

  .drawing-toolbar {
    top: 230px;
    right: 12px;
    left: 12px;
    justify-content: space-between;
  }

  .coordinate-panel {
    right: 12px;
    bottom: 12px;
    left: 12px;
  }
}
</style>
