// Types for our VITE_ environment variables (merged with Vite's built-in ImportMetaEnv).
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  // Only set for the public demo build. They must match app.demo.account in the backend.
  readonly VITE_DEMO_MOBILE?: string
  readonly VITE_DEMO_PASSWORD?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}