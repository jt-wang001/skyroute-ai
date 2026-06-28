<script setup lang="ts">
import { mockMissions } from '@/data/mockMissions'
import '@/styles/dashboard-command.css'

const stats = [
  { label: '任务总数', value: '24', trend: '+4', unit: '本周新增', tone: 'cyan' },
  { label: '规划航程', value: '186.4', trend: '+12.8', unit: 'km', tone: 'blue' },
  { label: '累计航点', value: '1,248', trend: '+96', unit: 'waypoints', tone: 'orange' },
  { label: '任务完成率', value: '92%', trend: '+3.2%', unit: '本月', tone: 'green' },
]

const telemetry = [
  { label: 'ALT', value: '86.4', unit: 'm' },
  { label: 'SPD', value: '8.2', unit: 'm/s' },
  { label: 'SAT', value: '19', unit: '' },
  { label: 'BAT', value: '87', unit: '%' },
]
</script>

<template>
  <div class="command-dashboard">
    <section class="flight-command">
      <div class="command-copy">
        <div class="live-badge"><span /> LIVE AIRSPACE</div>
        <p class="eyebrow">AUTONOMOUS MISSION CONTROL</p>
        <h2>驾驭低空 <span>智能航迹</span></h2>
        <p class="command-description">
          从区域识别到航线生成，让每一次巡检都具备可视化、可计算、可执行的数字航迹。
        </p>
        <div class="command-actions">
          <RouterLink class="launch-button" to="/missions/create">
            <span class="launch-icon">＋</span>创建飞行任务
          </RouterLink>
          <RouterLink class="ghost-button" to="/missions">查看任务矩阵 <span>→</span></RouterLink>
        </div>
        <div class="system-strip">
          <span><i class="system-ok" /> 飞控链路正常</span>
          <span><i /> GPS RTK FIX</span>
          <span><i /> 算法服务 READY</span>
        </div>
      </div>

      <div class="flight-stage">
        <div class="stage-grid" />
        <div class="stage-glow glow-one" />
        <div class="stage-glow glow-two" />

        <svg class="flight-path" viewBox="0 0 620 430" aria-hidden="true">
          <defs>
            <linearGradient id="routeGradient" x1="0" x2="1">
              <stop offset="0" stop-color="#36d6d0" stop-opacity="0" />
              <stop offset=".35" stop-color="#36d6d0" />
              <stop offset="1" stop-color="#ff9c57" />
            </linearGradient>
            <filter id="routeGlow">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
            </filter>
          </defs>
          <path class="route-shadow" d="M25 330 C110 260 150 370 238 288 S385 130 476 190 S555 132 605 72" />
          <path class="route-main" d="M25 330 C110 260 150 370 238 288 S385 130 476 190 S555 132 605 72" />
        </svg>

        <div class="drone-flight">
          <div class="drone-shadow" />
          <div class="drone">
            <span class="rotor rotor-one" /><span class="rotor rotor-two" />
            <span class="rotor rotor-three" /><span class="rotor rotor-four" />
            <span class="arm arm-one" /><span class="arm arm-two" />
            <span class="drone-body"><i class="camera-eye" /></span>
            <span class="drone-light light-left" /><span class="drone-light light-right" />
          </div>
        </div>

        <div class="target-lock lock-one"><span /><small>WP-04</small></div>
        <div class="target-lock lock-two"><span /><small>WP-05</small></div>

        <div class="telemetry-card">
          <div class="telemetry-head"><span>UAV-07 TELEMETRY</span><b>ONLINE</b></div>
          <div class="telemetry-grid">
            <div v-for="item in telemetry" :key="item.label">
              <small>{{ item.label }}</small>
              <strong>{{ item.value }}<i>{{ item.unit }}</i></strong>
            </div>
          </div>
          <div class="signal-bars">
            <span v-for="index in 18" :key="index" :style="{ height: `${8 + ((index * 7) % 20)}px` }" />
          </div>
        </div>

        <div class="hud-corner corner-top-left" /><div class="hud-corner corner-top-right" />
        <div class="hud-corner corner-bottom-left" /><div class="hud-corner corner-bottom-right" />
      </div>
    </section>

    <section class="command-stats">
      <article v-for="stat in stats" :key="stat.label" class="command-stat" :class="`tone-${stat.tone}`">
        <div class="stat-orbit"><span /><i /></div>
        <div><small>{{ stat.label }}</small><strong>{{ stat.value }}</strong></div>
        <p><b>{{ stat.trend }}</b> {{ stat.unit }}</p>
      </article>
    </section>

    <section class="mission-matrix content-card">
      <div class="section-heading">
        <div><p class="eyebrow">ACTIVE MISSION MATRIX</p><h3>任务运行矩阵</h3></div>
        <RouterLink class="matrix-link" to="/missions">进入任务中心 →</RouterLink>
      </div>
      <div class="mission-matrix-list">
        <RouterLink v-for="(mission, index) in mockMissions" :key="mission.id" class="matrix-row" :to="`/missions/${mission.id}`">
          <span class="matrix-number">0{{ index + 1 }}</span>
          <span class="matrix-route"><i /><b /><i /><b /><i /></span>
          <span class="matrix-title">
            <strong>{{ mission.name }}</strong>
            <small>ALT {{ mission.altitude }}M · SPD {{ mission.speed }}M/S</small>
          </span>
          <span class="matrix-data"><small>DISTANCE</small><b>{{ mission.distance }} km</b></span>
          <span class="matrix-data"><small>WAYPOINTS</small><b>{{ mission.waypointCount }}</b></span>
          <el-tag effect="plain">{{ mission.status }}</el-tag>
        </RouterLink>
      </div>
    </section>

    <section class="airspace-radar content-card">
      <div class="radar-heading">
        <div><p class="eyebrow">AIRSPACE RADAR</p><h3>实时空域扫描</h3></div>
        <span class="radar-status"><i /> SCANNING</span>
      </div>
      <div class="radar-screen">
        <div class="radar-circle radar-outer" /><div class="radar-circle radar-middle" />
        <div class="radar-circle radar-inner" /><div class="radar-axis axis-x" />
        <div class="radar-axis axis-y" /><div class="radar-sweep" />
        <span class="radar-object object-one"><i />UAV-03</span>
        <span class="radar-object object-two"><i />UAV-07</span>
        <span class="radar-object object-three"><i />UAV-12</span>
      </div>
      <div class="radar-footer">
        <span><b>06</b> 可执行</span><span><b>03</b> 飞行中</span><span><b>02</b> 待复核</span>
      </div>
    </section>
  </div>
</template>
