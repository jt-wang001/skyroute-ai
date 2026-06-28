<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const form = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 64, message: '用户名长度为 4-64 个字符', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        value === form.password ? callback() : callback(new Error('两次输入的密码不一致'))
      },
      trigger: 'blur',
    },
  ],
}

async function submit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await authStore.register({
      username: form.username,
      nickname: form.nickname,
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    await router.replace({ name: 'login' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page register">
    <section class="auth-visual">
      <div class="auth-visual-content">
        <span class="brand-mark large">SR</span>
        <p class="eyebrow">BUILD YOUR AIRSPACE</p>
        <h1>创建账户<br />开始规划航线</h1>
        <p>统一管理巡检区域、任务参数与航点数据，为后续 AI 参数解析预留完整能力。</p>
      </div>
    </section>

    <section class="auth-form-panel">
      <div class="auth-card">
        <p class="eyebrow">CREATE ACCOUNT</p>
        <h2>注册新用户</h2>
        <p class="muted">创建你的 SkyRoute AI 工作空间。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" placeholder="4-64 个字符" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" size="large" placeholder="平台显示名称" />
          </el-form-item>
          <div class="form-grid two">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" size="large" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" size="large" type="password" show-password />
            </el-form-item>
          </div>
          <el-button class="full-button" type="primary" size="large" :loading="submitting" @click="submit">
            创建账户
          </el-button>
        </el-form>

        <p class="auth-switch">
          已有账户？
          <RouterLink to="/login">返回登录</RouterLink>
        </p>
      </div>
    </section>
  </div>
</template>
