import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const proxyTarget = process.env.BACKEND_URL || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/Auth': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/Expense': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/Dashboard': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/Rank': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/Shift': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      },
      '/House': {
        target: proxyTarget,
        changeOrigin: true,
        secure: false
      }
    }
  },
  preview: {
    port: 4173
  }
});
