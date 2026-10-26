import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Tracks 12 (JavaScript/TypeScript) and 13 (React). Run one level: npx vitest run src/js/l1
export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
});
