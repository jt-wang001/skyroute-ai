import { createRouter, createWebHistory } from 'vue-router'
import { tokenStorage } from '@/utils/storage'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/auth/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: 'Dashboard' },
        },
        {
          path: 'missions',
          name: 'missions',
          component: () => import('@/views/mission/MissionListView.vue'),
          meta: { title: '任务列表' },
        },
        {
          path: 'missions/create',
          name: 'mission-create',
          component: () => import('@/views/mission/MissionCreateView.vue'),
          meta: { title: '创建任务' },
        },
        {
          path: 'missions/:id',
          name: 'mission-detail',
          component: () => import('@/views/mission/MissionDetailView.vue'),
          meta: { title: '任务详情' },
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const hasToken = Boolean(tokenStorage.get())
  if (to.matched.some((record) => record.meta.requiresAuth) && !hasToken) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }
  if (to.meta.guestOnly && hasToken) {
    return { name: 'dashboard' }
  }
  return true
})

router.afterEach((to) => {
  document.title = `${String(to.meta.title || 'SkyRoute AI')} · SkyRoute AI`
})

export default router
