<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import '@/styles/login-cinematic.css'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const particles = Array.from({ length: 28 }, (_, index) => ({
  left: `${(index * 37) % 100}%`,
  top: `${(index * 53) % 100}%`,
  delay: `${(index % 9) * -0.7}s`,
  duration: `${4 + (index % 6)}s`,
}))

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="cinematic-login">
    <div class="cinematic-stars">
      <i
        v-for="(particle, index) in particles"
        :key="index"
        :style="{
          left: particle.left,
          top: particle.top,
          animationDelay: particle.delay,
          animationDuration: particle.duration,
        }"
      />
    </div>
    <div class="cinematic-grid" />
    <div class="cinematic-aurora aurora-cyan" />
    <div class="cinematic-aurora aurora-orange" />

    <section class="login-flight-scene">
      <header class="scene-brand">
        <span class="brand-mark large">SR</span>
        <div>
          <strong>SKYROUTE AI</strong>
          <small>AUTONOMOUS FLIGHT SYSTEM</small>
        </div>
      </header>

      <div class="scene-copy">
        <div class="scene-status"><i /> AIRSPACE ONLINE</div>
        <p class="eyebrow">INTELLIGENT LOW-ALTITUDE OPERATIONS</p>
        <h1>穿越云层<br /><span>定义航迹</span></h1>
        <p>面向低空巡检任务的一体化智能规划平台，让区域、参数和航点在同一个数字空间协同运行。</p>
      </div>

      <svg class="cinematic-route" viewBox="0 0 900 590" aria-hidden="true">
        <defs>
          <linearGradient id="loginRoute" x1="0" x2="1">
            <stop offset="0" stop-color="#36d6d0" stop-opacity="0" />
            <stop offset=".45" stop-color="#36d6d0" />
            <stop offset="1" stop-color="#ff9c57" />
          </linearGradient>
          <filter id="loginRouteGlow">
            <feGaussianBlur stdDeviation="3" result="blur" />
            <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
          </filter>
        </defs>
        <path class="cinematic-route-glow" d="M-40 520 C170 390 245 540 398 376 S650 170 940 90" />
        <path class="cinematic-route-line" d="M-40 520 C170 390 245 540 398 376 S650 170 940 90" />
      </svg>

      <div class="login-drone-flight">
        <div class="login-drone-shadow" />
        <div class="login-drone">
          <span class="login-rotor lr-one" /><span class="login-rotor lr-two" />
          <span class="login-rotor lr-three" /><span class="login-rotor lr-four" />
          <span class="login-arm la-one" /><span class="login-arm la-two" />
          <span class="login-drone-body"><i /></span>
          <span class="login-nav-light nav-green" /><span class="login-nav-light nav-red" />
        </div>
      </div>

      <div class="login-radar">
        <span class="login-radar-ring ring-a" />
        <span class="login-radar-ring ring-b" />
        <span class="login-radar-ring ring-c" />
        <i class="login-radar-sweep" />
        <b class="radar-target target-a" />
        <b class="radar-target target-b" />
      </div>

      <div class="scene-telemetry">
        <div><small>ALTITUDE</small><strong>86.4 <i>M</i></strong></div>
        <div><small>AIRSPEED</small><strong>8.2 <i>M/S</i></strong></div>
        <div><small>SATELLITES</small><strong>19</strong></div>
        <div><small>BATTERY</small><strong>87 <i>%</i></strong></div>
      </div>

      <div class="scene-footer">
        <span><i /> LINK STABLE</span>
        <span>LAT 39.9075° N</span>
        <span>LON 116.3912° E</span>
      </div>
    </section>

    <section class="login-console">
      <div class="console-corner console-top-left" />
      <div class="console-corner console-top-right" />
      <div class="console-corner console-bottom-left" />
      <div class="console-corner console-bottom-right" />

      <div class="console-header">
        <span class="console-index">SR / 01</span>
        <span class="console-signal"><i v-for="index in 5" :key="index" /></span>
      </div>

      <div class="console-content">
        <p class="eyebrow">SECURE ACCESS TERMINAL</p>
        <h2>欢迎返航</h2>
        <p class="console-intro">验证飞行员身份，进入 SkyRoute AI 指挥中心。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="操作员账号" prop="username">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名">
              <template #prefix><span class="input-code">ID</span></template>
            </el-input>
          </el-form-item>
          <el-form-item label="访问密钥" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              placeholder="请输入密码"
            >
              <template #prefix><span class="input-code">KY</span></template>
            </el-input>
          </el-form-item>
          <el-button class="console-login-button" type="primary" size="large" :loading="submitting" @click="submit">
            <span>建立安全连接</span>
            <i>→</i>
          </el-button>
        </el-form>

        <div class="console-divider"><span>NEW OPERATOR</span></div>
        <RouterLink class="console-register-link" to="/register">注册新的飞行员账户</RouterLink>
      </div>

      <footer class="console-footer">
        <span><i /> ENCRYPTED</span>
        <span>JWT AUTH PROTOCOL</span>
      </footer>
    </section>
  </main>
</template>
