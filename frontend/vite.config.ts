import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.API_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
    watch: {
      // Docker Desktop on Windows needs polling to detect host file changes.
      usePolling: process.env.USE_POLLING === 'true',
      interval: 300,
    },
  },
})
