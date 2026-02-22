import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backendTarget = 'http://localhost:8090';

function makeProxyConfig(target: string) {
  return {
    '/api': {
      target,
      changeOrigin: true,
      configure: (proxy: any) => {
        proxy.on('proxyReq', (proxyReq: any, req: any) => {
          // Forward the real client IP so the backend can geolocate it correctly
          const forwarded = req.headers['x-forwarded-for'] as string | undefined;
          const clientIp = forwarded ? forwarded.split(',')[0].trim() : (req.socket?.remoteAddress ?? '');
          proxyReq.setHeader('X-Forwarded-For', clientIp);
          proxyReq.setHeader('X-Real-IP', clientIp);
        });
      },
    },
    '/health': {
      target,
      changeOrigin: true,
    },
  };
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: makeProxyConfig(backendTarget),
  },
  preview: {
    proxy: makeProxyConfig(backendTarget),
  },
})
