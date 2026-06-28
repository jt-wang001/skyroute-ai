import http from './http'
import type {
  ApiResult,
  AuthData,
  LoginPayload,
  RegisterPayload,
  UserProfile,
} from '@/types/api'

export const register = (payload: RegisterPayload) =>
  http.post<ApiResult<number>>('/auth/register', payload)

export const login = (payload: LoginPayload) =>
  http.post<ApiResult<AuthData>>('/auth/login', payload)

export const getProfile = () =>
  http.get<ApiResult<UserProfile>>('/user/profile')
