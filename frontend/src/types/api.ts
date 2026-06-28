export interface ApiResult<T> {
  code: number
  message: string
  data: T
  timestamp: string
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload extends LoginPayload {
  nickname: string
}

export interface AuthData {
  token: string
  userId: number
  nickname: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  createTime: string
}
