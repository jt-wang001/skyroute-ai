const TOKEN_KEY = 'skyroute_access_token'
const USER_KEY = 'skyroute_user'

export interface StoredUser {
  userId: number
  nickname: string
}

export const tokenStorage = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

export const userStorage = {
  get(): StoredUser | null {
    const value = localStorage.getItem(USER_KEY)
    if (!value) return null
    try {
      return JSON.parse(value) as StoredUser
    } catch {
      localStorage.removeItem(USER_KEY)
      return null
    }
  },
  set(user: StoredUser) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },
  clear() {
    localStorage.removeItem(USER_KEY)
  },
}
