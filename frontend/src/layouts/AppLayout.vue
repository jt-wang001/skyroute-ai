<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/missions/create')) return '/missions/create'
  if (route.path.startsWith('/missions')) return '/missions'
  return '/'
})

onMounted(() => {
  if (!authStore.profile) {
    authStore.loadProfile().catch(() => undefined)
  }
})

function logout() {
  authStore.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark">SR</span>
        <span>
          <strong>SkyRoute AI</strong>
          <small>无人机任务规划</small>
        </span>
      </RouterLink>

      <el-menu :default-active="activeMenu" router>
        <el-menu-item index="/">
          <span>控制台</span>
        </el-menu-item>
        <el-menu-item index="/missions">
          <span>任务列表</span>
        </el-menu-item>
        <el-menu-item index="/missions/create">
          <span>创建任务</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-foot">
        <span class="status-dot" />
        <span>系统服务正常</span>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar">
        <div>
          <p class="eyebrow">LOW-ALTITUDE OPERATIONS</p>
          <h1>{{ route.meta.title || 'Dashboard' }}</h1>
        </div>
        <el-dropdown trigger="click">
          <button class="user-chip">
            <span class="avatar">{{ authStore.nickname.slice(0, 1) || 'U' }}</span>
            <span>{{ authStore.nickname || '用户' }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <section class="page-content">
        <RouterView />
      </section>
    </main>
  </div>
</template>
