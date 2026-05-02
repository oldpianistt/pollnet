import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
  build: {
    rollupOptions: {
      output: {
        // Pull the heavy libraries into their own chunks so first paint
        // doesn't pay the cost of downloading Recharts on /login.
        manualChunks: {
          'react-vendor':  ['react', 'react-dom', 'react-router-dom'],
          'recharts':      ['recharts'],
          'query-vendor':  ['@tanstack/react-query', 'axios', 'zustand'],
        },
      },
    },
    chunkSizeWarningLimit: 600,
  },
});
