import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Testes unitários e de componente (Vitest + Testing Library). Os testes de sistema ficam em tests/e2e (Playwright).
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    include: ["tests/unit/**/*.test.{ts,tsx}"],
    setupFiles: ["tests/unit/setup.ts"],
    env: { VITE_API_URL: "http://api.test" },
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/main.tsx", "src/vite-env.d.ts"],
      reporter: ["text-summary", "html", "json-summary", "lcov"],
      reportsDirectory: "coverage",
    },
  },
});
