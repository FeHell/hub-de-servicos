import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import type { UserConfig } from 'vitest/config'

export default defineConfig(({ mode }) => {
  const ambiente = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: ambiente.BACKEND_URL || 'http://127.0.0.1:8080',
          changeOrigin: true,
        },
      },
    },
    test: {
      environment: 'jsdom',
      clearMocks: true,
      setupFiles: ['./src/test/configurar.ts'],
    },
  } satisfies UserConfig
})
