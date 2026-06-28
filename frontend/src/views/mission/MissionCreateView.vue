<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MapView, { type MapCoordinate } from '@/components/MapView.vue'
import { createMission } from '@/api/mission'
import type { GeoJsonPolygon } from '@/types/mission'

const router = useRouter()
const selectedCoordinate = ref<MapCoordinate | null>(null)
const areaGeoJson = ref<GeoJsonPolygon | null>(null)
const submitting = ref(false)

const form = reactive({
  missionName: '',
  flightAltitude: 80,
  flightSpeed: 8,
  headingOverlapRate: 75,
  sideOverlapRate: 65,
})

const vertexCount = computed(() => {
  const ring = areaGeoJson.value?.coordinates[0]
  return ring ? Math.max(ring.length - 1, 0) : 0
})

function handleCoordinateChange(coordinate: MapCoordinate) {
  selectedCoordinate.value = coordinate
}

function handleAreaChange(geoJson: GeoJsonPolygon | null) {
  areaGeoJson.value = geoJson
  if (!geoJson) selectedCoordinate.value = null
}

async function submitMission() {
  if (!form.missionName.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  if (!areaGeoJson.value) {
    ElMessage.warning('请至少绘制 3 个巡检区域顶点')
    return
  }

  submitting.value = true
  try {
    const response = await createMission({
      missionName: form.missionName.trim(),
      flightAltitude: form.flightAltitude,
      flightSpeed: form.flightSpeed,
      headingOverlapRate: form.headingOverlapRate,
      sideOverlapRate: form.sideOverlapRate,
      areaGeojson: areaGeoJson.value,
    })
    ElMessage.success('任务创建成功')
    await router.push({ name: 'mission-detail', params: { id: response.data.data } })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="create-layout">
    <section class="content-card map-workspace">
      <div class="section-heading">
        <div>
          <p class="eyebrow">AREA DESIGNER</p>
          <h2>绘制巡检区域</h2>
        </div>
        <el-tag :type="areaGeoJson ? 'success' : 'info'" effect="plain">
          {{ areaGeoJson ? `Polygon · ${vertexCount} 个顶点` : '等待绘制' }}
        </el-tag>
      </div>

      <MapView @coordinate-change="handleCoordinateChange" @area-change="handleAreaChange" />
      <p class="map-tip">依次点击地图添加顶点，至少 3 个顶点后自动形成巡检区域。</p>
    </section>

    <section class="content-card parameter-panel">
      <p class="eyebrow">FLIGHT PARAMETERS</p>
      <h2>任务参数</h2>

      <div class="selected-position" :class="{ empty: !selectedCoordinate }">
        <span>最后添加的顶点</span>
        <template v-if="selectedCoordinate">
          <strong>{{ selectedCoordinate.longitude }}</strong>
          <strong>{{ selectedCoordinate.latitude }}</strong>
        </template>
        <small v-else>请点击左侧地图添加区域顶点</small>
      </div>

      <el-form :model="form" label-position="top" @submit.prevent="submitMission">
        <el-form-item label="任务名称" required>
          <el-input
            v-model="form.missionName"
            maxlength="100"
            placeholder="例如：东区光伏板巡检"
            show-word-limit
          />
        </el-form-item>
        <div class="form-grid two">
          <el-form-item label="飞行高度（m）" required>
            <el-input-number v-model="form.flightAltitude" :min="1" :max="500" />
          </el-form-item>
          <el-form-item label="飞行速度（m/s）" required>
            <el-input-number v-model="form.flightSpeed" :min="0.1" :max="30" :step="0.5" />
          </el-form-item>
          <el-form-item label="航向重叠率（%）" required>
            <el-input-number v-model="form.headingOverlapRate" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="旁向重叠率（%）" required>
            <el-input-number v-model="form.sideOverlapRate" :min="0" :max="100" />
          </el-form-item>
        </div>
      </el-form>

      <div class="estimate-box">
        <span>区域数据</span>
        <strong>{{ areaGeoJson ? `${vertexCount} 个顶点` : '未生成' }}</strong>
        <small>
          {{ areaGeoJson ? 'GeoJSON Polygon 已闭合，可创建任务' : '至少添加 3 个顶点' }}
        </small>
      </div>

      <div class="action-row">
        <el-button @click="router.push({ name: 'missions' })">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMission">
          创建任务
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.map-tip {
  margin: 12px 0 0;
  color: #8096aa;
  font-size: 12px;
}

.selected-position {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
  margin: 20px 0 24px;
  padding: 15px 17px;
  border: 1px solid rgba(54, 214, 208, 0.25);
  border-left: 3px solid #36d6d0;
  border-radius: 6px;
  background: rgba(54, 214, 208, 0.06);
}

.selected-position > span {
  grid-column: 1 / -1;
  color: #8096aa;
  font-size: 11px;
}

.selected-position strong {
  color: #36d6d0;
  font: 13px monospace;
}

.selected-position small {
  grid-column: 1 / -1;
  color: #8096aa;
}

.selected-position.empty {
  border-left-color: #8096aa;
  background: rgba(128, 150, 170, 0.05);
}
</style>
