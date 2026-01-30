/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_HTTP_DEBUG?: string
  readonly VITE_PASSWORD_PUBLIC_KEY?: string
  readonly VITE_WS_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

