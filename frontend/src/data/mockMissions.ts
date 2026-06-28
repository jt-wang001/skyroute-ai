import type { Mission } from '@/types/mission'

export const mockMissions: Mission[] = [
  {
    id: 1001,
    name: '东区光伏板巡检',
    status: 'PLANNED',
    altitude: 80,
    speed: 8,
    distance: 12.6,
    waypointCount: 48,
    updatedAt: '2026-06-25 16:20',
  },
  {
    id: 1002,
    name: '北侧输电走廊复检',
    status: 'EXECUTING',
    altitude: 100,
    speed: 10,
    distance: 18.3,
    waypointCount: 67,
    updatedAt: '2026-06-25 15:42',
  },
  {
    id: 1003,
    name: '水库岸线例行任务',
    status: 'COMPLETED',
    altitude: 70,
    speed: 7.5,
    distance: 9.8,
    waypointCount: 39,
    updatedAt: '2026-06-24 18:05',
  },
]
