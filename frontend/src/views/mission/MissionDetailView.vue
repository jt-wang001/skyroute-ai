<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMission, exportMissionGeoJson, getMission } from '@/api/mission'
import MissionRouteMap from '@/components/MissionRouteMap.vue'
import type { MissionDetail, MissionStatus, RiskLevel } from '@/types/mission'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const deleting = ref(false)
const exporting = ref(false)
const mission = ref<MissionDetail | null>(null)


const riskMeta: Record<RiskLevel, { label: string; type: 'success' | 'warning' | 'danger' }> = {
  LOW: { label: '低风险', type: 'success' },
  MEDIUM: { label: '中风险', type: 'warning' },
  HIGH: { label: '高风险', type: 'danger' },
}
const statusMeta: Record<MissionStatus, { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger' }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PLANNED: { label: '已规划', type: 'primary' },
  EXECUTING: { label: '执行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  FAILED: { label: '失败', type: 'danger' },
}

const sortedWaypoints = computed(() => {
  return [...(mission.value?.waypoints || [])].sort((a, b) => a.sequenceNo - b.sequenceNo)
})

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function formatDuration(seconds?: number) {
  const total = Number(seconds || 0)
  if (total < 60) return `${total.toFixed(0)} 秒`

  const hours = Math.floor(total / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const remainingSeconds = Math.floor(total % 60)

  if (hours > 0) return `${hours} 小时 ${minutes} 分钟`
  if (minutes > 0) return `${minutes} 分钟 ${remainingSeconds} 秒`
  return `${remainingSeconds} 秒`
}

function formatDistance(meters?: number) {
  const value = Number(meters || 0)
  if (value >= 1000) return `${(value / 1000).toFixed(2)} km`
  return `${value.toFixed(2)} m`
}

async function loadMission() {
  const id = Number(route.params.id)
  if (!Number.isInteger(id) || id <= 0) {
    ElMessage.error('任务 ID 无效')
    await router.replace({ name: 'missions' })
    return
  }

  loading.value = true
  try {
    const response = await getMission(id)
    mission.value = response.data.data
  } catch {
    await router.replace({ name: 'missions' })
  } finally {
    loading.value = false
  }
}

async function exportGeoJson() {
  if (!mission.value) return

  exporting.value = true
  try {
    const response = await exportMissionGeoJson(mission.value.id)
    const blob = new Blob([response.data], {
      type: 'application/geo+json;charset=utf-8',
    })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `mission-${mission.value.id}.geojson`
    anchor.click()
    URL.revokeObjectURL(url)
    ElMessage.success('GeoJSON 已导出')
  } finally {
    exporting.value = false
  }
}

async function handleDelete() {
  if (!mission.value) return

  try {
    await ElMessageBox.confirm(
      `删除后无法恢复，确定删除“${mission.value.missionName}”吗？`,
      '删除任务',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  deleting.value = true
  try {
    await deleteMission(mission.value.id)
    ElMessage.success('任务已删除')
    await router.push({ name: 'missions' })
  } finally {
    deleting.value = false
  }
}

onMounted(loadMission)
watch(() => route.params.id, loadMission)
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <template v-if="mission">
      <section class="content-card detail-hero">
        <div>
          <p class="eyebrow">MISSION #{{ mission.id }}</p>
          <h2>{{ mission.missionName }}</h2>
          <p>{{ formatDateTime(mission.updateTime) }} 更新</p>
        </div>
        <div class="hero-actions">
          <el-tag :type="statusMeta[mission.status].type" effect="dark">
            {{ statusMeta[mission.status].label }}
          </el-tag>
          <el-tag :type="riskMeta[mission.riskAssessment.riskLevel].type" effect="dark">
            {{ riskMeta[mission.riskAssessment.riskLevel].label }}
          </el-tag>
          <el-button :loading="exporting" @click="exportGeoJson">导出 GeoJSON</el-button>
          <el-button type="danger" plain :loading="deleting" @click="handleDelete">
            删除任务
          </el-button>
        </div>
      </section>

      <section class="detail-main-grid">
        <div class="content-card map-card">
          <div class="section-heading">
            <div>
              <p class="eyebrow">ROUTE MAP</p>
              <h3>巡检区域与规划航线</h3>
            </div>
            <span class="muted">点击航点查看经纬度、高度和序号</span>
          </div>

          <MissionRouteMap
            :area-geojson="mission.areaGeojson"
            :waypoints="sortedWaypoints"
          />
        </div>

        <aside class="content-card mission-summary-panel">
          <p class="eyebrow">FLIGHT SUMMARY</p>
          <h3>任务飞行参数</h3>

          <dl class="summary-list">
            <div>
              <dt>飞行高度</dt>
              <dd>{{ mission.flightAltitude }} m</dd>
            </div>
            <div>
              <dt>飞行速度</dt>
              <dd>{{ mission.flightSpeed }} m/s</dd>
            </div>
            <div>
              <dt>航点数</dt>
              <dd>{{ mission.waypointCount }}</dd>
            </div>
            <div>
              <dt>总航程</dt>
              <dd>{{ formatDistance(mission.totalDistance) }}</dd>
            </div>
            <div>
              <dt>预计时间</dt>
              <dd>{{ formatDuration(mission.estimatedDurationSec) }}</dd>
            </div>
            <div>
              <dt>航向重叠率</dt>
              <dd>{{ mission.headingOverlapRate }}%</dd>
            </div>
            <div>
              <dt>旁向重叠率</dt>
              <dd>{{ mission.sideOverlapRate }}%</dd>
            </div>
            <div>
              <dt>创建时间</dt>
              <dd>{{ formatDateTime(mission.createTime) }}</dd>
            </div>
          </dl>

          <div class="risk-panel" :class="'risk-' + mission.riskAssessment.riskLevel.toLowerCase()">
            <div class="risk-heading">
              <span>风险评估</span>
              <el-tag :type="riskMeta[mission.riskAssessment.riskLevel].type" effect="dark">
                {{ riskMeta[mission.riskAssessment.riskLevel].label }}
              </el-tag>
            </div>
            <ul>
              <li v-for="message in mission.riskAssessment.riskMessages" :key="message">
                {{ message }}
              </li>
            </ul>
          </div>
        </aside>
      </section>

      <section class="content-card waypoint-card">
        <div class="section-heading">
          <div>
            <p class="eyebrow">WAYPOINTS</p>
            <h3>航点列表</h3>
          </div>
          <span class="muted">共 {{ sortedWaypoints.length }} 个航点</span>
        </div>

        <el-table :data="sortedWaypoints" height="360">
          <el-table-column prop="sequenceNo" label="#" width="70" />
          <el-table-column prop="longitude" label="经度" />
          <el-table-column prop="latitude" label="纬度" />
          <el-table-column prop="altitude" label="高度(m)" width="100" />
          <el-table-column prop="actionType" label="动作" width="110" />
        </el-table>
      </section>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  display: grid;
  gap: 20px;
}

.detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.detail-hero h2 {
  margin: 4px 0 8px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
}

.detail-main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
  align-items: start;
}

.map-card {
  min-width: 0;
}

.section-heading {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.section-heading h3 {
  margin: 4px 0 0;
}

.mission-summary-panel {
  position: sticky;
  top: 20px;
}

.mission-summary-panel h3 {
  margin: 4px 0 18px;
}

.summary-list {
  display: grid;
  gap: 12px;
  margin: 0;
}

.summary-list div {
  padding: 14px;
  border: 1px solid rgba(54, 214, 208, 0.14);
  border-radius: 10px;
  background: rgba(4, 18, 31, 0.56);
}

.summary-list dt {
  margin-bottom: 6px;
  color: #7f96aa;
  font-size: 12px;
}

.summary-list dd {
  margin: 0;
  color: #e7f2fb;
  font-size: 18px;
  font-weight: 700;
}

.muted {
  color: #7f96aa;
  font-size: 13px;
}


.risk-panel {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid rgba(54, 214, 208, 0.14);
  border-radius: 10px;
  background: rgba(4, 18, 31, 0.56);
}

.risk-panel.risk-low {
  border-color: rgba(103, 194, 58, 0.32);
}

.risk-panel.risk-medium {
  border-color: rgba(230, 162, 60, 0.38);
}

.risk-panel.risk-high {
  border-color: rgba(245, 108, 108, 0.45);
}

.risk-heading {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  color: #e7f2fb;
  font-weight: 700;
}

.risk-panel ul {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
  color: #9fb3c8;
  font-size: 13px;
  line-height: 1.5;
}
@media (max-width: 1080px) {
  .detail-main-grid {
    grid-template-columns: 1fr;
  }

  .mission-summary-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .detail-hero,
  .section-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>




