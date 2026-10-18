import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// The dev server proxies API calls to the backend started with `./gradlew bootRun`.
const backend = 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/openapi': backend,
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
});
