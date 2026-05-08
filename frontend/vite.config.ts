import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, __dirname, '');
  const apiUrl = env.VITE_API_URL || 'http://localhost:8091';
  const mcpApiUrl = env.VITE_MCP_API_URL || 'http://localhost:8092';

  return {
    plugins: [react()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-react': ['react', 'react-dom', 'react-router-dom'],
            'vendor-mui': ['@mui/material', '@mui/icons-material'],
            'vendor-query': ['@tanstack/react-query'],
            'vendor-markdown': ['react-markdown', 'react-syntax-highlighter', 'remark-gfm'],
            'vendor-motion': ['framer-motion'],
          },
        },
      },
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: apiUrl,
          changeOrigin: true,
        },
        '/mcp-api': {
          target: mcpApiUrl,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/mcp-api/, '/api'),
        },
      },
    },
  };
});
