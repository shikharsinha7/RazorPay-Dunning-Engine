import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Lets the frontend call fetch('/api/...') during local dev without any
    // backend CORS config — Vite proxies to the Spring Boot app so the
    // browser sees it as same-origin. Production deploy (Day 7) will need
    // a real CORS config or same-origin hosting instead of this proxy.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
