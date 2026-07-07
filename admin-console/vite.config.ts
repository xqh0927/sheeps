import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Cloudflare Pages 构建：npm run build -> dist
export default defineConfig({
  plugins: [react()],
  server: { port: 5173 },
  build: { outDir: 'dist' },
});
