// https://vite.dev/config/
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { viteCommonjs } from '@originjs/vite-plugin-commonjs'

export default defineConfig({
  plugins: [
    react(),
    viteCommonjs()
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      '/ws': {  // Add WebSocket proxy
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        ws: true,  // Enable WebSocket proxying
      }
    }
  },
  define: {
    global: 'window', // Add this
  }
})
