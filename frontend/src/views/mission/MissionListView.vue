<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMission, listMissions } from '@/api/mission'
import type { MissionListItem, MissionStatus } from '@/types/mission'

const keyword = ref('')
const status = ref<MissionStatus | ''>('')
const loading = ref(false)
const deletingId = ref<number | null>(null)
const missions = ref<MissionListItem[]>([])

const statusMeta: Record<MissionStatus, { label: string; type: 'info' | 'primary' | 'warning' | 'success' | 'danger' }> = {
  DRAFT: { label: '草稿', type: 'info' },
  PLANNED: { label: '已规划', type: 'primary' },
  EXECUTING: { label: '执行中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
  FAILED: { label: '失败', type: 'danger' },
}

const filteredMissions = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return missions.value.filter((mission) => {
    const matchesKeyword = mission.missionName.toLowerCase().includes(normalizedKeyword)
    return matchesKeyword && (!status.value || mission.status === status.value)
  })
})

function formatDistance(meters: number) {
  return `${(Number(meters || 0) / 1000).toFixed(2)} km`
}

function formatDateTime(value: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function loadMissions() {
  loading.value = true
  try {
    const response = await listMissions()
    missions.value = response.data.data || []
  } finally {
    loading.value = false
  }
}

async function handleDelete(mission: MissionListItem) {
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复，确定删除“${mission.missionName}”吗？`,
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

  deletingId.value = mission.id
  try {
    await deleteMission(mission.id)
    missions.value = missions.value.filter((item) => item.id !== mission.id)
    ElMessage.success('任务已删除')
  } finally {
    deletingId.value = null
  }
}

onMounted(loadMissions)
</script>

<template>
  <section class="content-card">
    <div class="section-heading">
      <div>
        <p class="eyebrow">MISSION LIBRARY</p>
        <h2>飞行任务</h2>
      </div>
      <RouterLink class="el-button el-button--primary" :to="{ name: 'mission-create' }">
        创建任务
      </RouterLink>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" clearable placeholder="搜索任务名称" />
      <el-select v-model="status" clearable placeholder="全部状态">
        <el-option
          v-for="(meta, value) in statusMeta"
          :key="value"
          :label="meta.label"
          :value="value"
        />
      </el-select>
      <el-button :loading="loading" @click="loadMissions">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="filteredMissions" class="mission-table">
      <el-table-column label="任务名称" min-width="220">
        <template #default="{ row }">
          <RouterLink class="table-link" :to="{ name: 'mission-detail', params: { id: row.id } }">
            {{ row.missionName }}
          </RouterLink>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusMeta[row.status as MissionStatus].type" effect="plain">
            {{ statusMeta[row.status as MissionStatus].label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="flightAltitude" label="高度（m）" width="110" />
      <el-table-column prop="flightSpeed" label="速度（m/s）" width="120" />
      <el-table-column label="总航程" width="120">
        <template #default="{ row }">{{ formatDistance(row.totalDistance) }}</template>
      </el-table-column>
      <el-table-column prop="waypointCount" label="航点" width="90" />
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="170">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="$router.push({ name: 'mission-detail', params: { id: row.id } })"
          >
            详情
          </el-button>
          <el-button
            link
            type="danger"
            :loading="deletingId === row.id"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无任务，点击右上角创建任务" />
      </template>
    </el-table>
  </section>
</template>
