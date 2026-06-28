export type MissionStatus =
  | 'DRAFT'
  | 'PLANNED'
  | 'EXECUTING'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'FAILED'

export interface GeoJsonPolygon {
  type: 'Polygon'
  coordinates: [number, number][][]
}

export interface MissionCreatePayload {
  missionName: string
  flightAltitude: number
  flightSpeed: number
  headingOverlapRate: number
  sideOverlapRate: number
  areaGeojson: GeoJsonPolygon
}

export interface MissionListItem {
  id: number
  missionName: string
  flightAltitude: number
  flightSpeed: number
  headingOverlapRate: number
  sideOverlapRate: number
  totalDistance: number
  estimatedDurationSec: number
  waypointCount: number
  status: MissionStatus
  createTime: string
  updateTime: string
}

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface RiskAssessment {
  riskLevel: RiskLevel
  riskMessages: string[]
}

export interface MissionWaypoint {
  id: number
  sequenceNo: number
  longitude: number
  latitude: number
  altitude: number
  actionType: string
}

export interface MissionDetail extends MissionListItem {
  userId: number
  areaGeojson: GeoJsonPolygon
  waypoints: MissionWaypoint[]
  riskAssessment: RiskAssessment
}

// Dashboard demo type; keep until dashboard switches to real API.
export interface Mission {
  id: number
  name: string
  status: MissionStatus
  altitude: number
  speed: number
  distance: number
  waypointCount: number
  updatedAt: string
}

