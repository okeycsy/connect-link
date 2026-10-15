/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// 개발 서버에서는 /api 와 /v3 를 8080(Spring Boot)으로 넘긴다.
// 프로덕션 빌드(dist/)는 backend의 app.jar 안에 정적 자원으로 들어간다.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/v3': 'http://localhost:8080',
    },
  },
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        // 자주 안 바뀌는 라이브러리를 따로 묶어 캐시가 오래 가게 한다.
        manualChunks: {
          react: ['react', 'react-dom', 'react-router-dom', '@tanstack/react-query'],
          mantine: ['@mantine/core', '@mantine/hooks', '@mantine/form', '@mantine/notifications'],
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: false,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
});
