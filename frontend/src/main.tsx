import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@fontsource-variable/inter'
import './index.css'
import App from './App.tsx'
import { configureHttp } from './shared/api/http'
import { sessionStore } from './features/auth/sessionStore.ts'

// Connect the HTTP client to the session once, before anything renders.
configureHttp({
  getAccessToken: sessionStore.getAccessToken,
  onUnauthorized: () => sessionStore.signOut('expired'),
})

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)