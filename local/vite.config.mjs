import frontendConfig from '/app/vite.config.ts';

export default {
  ...frontendConfig,
  server: {
    ...frontendConfig.server,
    proxy: {
      '/api': 'http://backend:8080',
      '/scraper': {
        target: 'http://band-py:8000',
        rewrite: path => path.replace(/^\/scraper/, ''),
      },
    },
    watch: { usePolling: true, interval: 500 },
  },
};
