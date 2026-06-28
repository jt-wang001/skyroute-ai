import http from './http'
import type { ApiResult } from '@/types/api'
import type {
  MissionCreatePayload,
  MissionDetail,
  MissionListItem,
} from '@/types/mission'

export const createMission = (payload: MissionCreatePayload) =>
  http.post<ApiResult<number>>('/missions', payload)

export const listMissions = () =>
  http.get<ApiResult<MissionListItem[]>>('/missions')

export const getMission = (id: number) =>
  http.get<ApiResult<MissionDetail>>(`/missions/${id}`)

export const deleteMission = (id: number) =>
  http.delete<ApiResult<null>>(`/missions/${id}`)

export const exportMissionGeoJson = (id: number) =>
  http.get<Blob>('/missions/' + id + '/export/geojson', {
    responseType: 'blob',
  })


