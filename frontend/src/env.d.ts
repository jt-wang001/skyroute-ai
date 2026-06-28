/// <reference types="vite/client" />
/// <reference types="@amap/amap-jsapi-types" />

interface Window {
  _AMapSecurityConfig?: {
    securityJsCode?: string
    serviceHost?: string
  }
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_AMAP_KEY?: string
  readonly VITE_AMAP_SECURITY_CODE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
