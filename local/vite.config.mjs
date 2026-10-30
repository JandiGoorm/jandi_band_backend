import frontendConfig from '/app/vite.config.ts';

export default {
  ...frontendConfig,
  server: {
    ...frontendConfig.server,
    proxy: {
      '/api': 'http://backend:8080',
    },
    watch: { usePolling: true, interval: 500 },
  },
};
