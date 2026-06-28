import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { LoginPayload, RegisterPayload, UserProfile } from '@/types/api'
import { tokenStorage, userStorage } from '@/utils/storage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(tokenStorage.get())
  const storedUser = userStorage.get()
  const userId = ref<number | null>(storedUser?.userId ?? null)
  const nickname = ref(storedUser?.nickname ?? '')
  const profile = ref<UserProfile | null>(null)

  const isAuthenticated = computed(() => Boolean(token.value))

  async function register(payload: RegisterPayload) {
    return authApi.register(payload)
  }

  async function login(payload: LoginPayload) {
    const { data } = await authApi.login(payload)
    token.value = data.data.token
    userId.value = data.data.userId
    nickname.value = data.data.nickname
    tokenStorage.set(data.data.token)
    userStorage.set({
      userId: data.data.userId,
      nickname: data.data.nickname,
    })
    return data.data
  }

  async function loadProfile() {
    const { data } = await authApi.getProfile()
    profile.value = data.data
    nickname.value = data.data.nickname
    userStorage.set({
      userId: data.data.id,
      nickname: data.data.nickname,
    })
    return data.data
  }

  function logout() {
    token.value = null
    userId.value = null
    nickname.value = ''
    profile.value = null
    tokenStorage.clear()
    userStorage.clear()
  }

  return {
    token,
    userId,
    nickname,
    profile,
    isAuthenticated,
    register,
    login,
    loadProfile,
    logout,
  }
})
