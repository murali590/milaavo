// Types for our VITE_ environment variables (merged with Vite's built-in ImportMetaEnv).
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}