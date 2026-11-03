import { defineConfig, devices } from "@playwright/test";

// Testes de sistema e aceitação (Playwright): sobem a API de verdade (perfil h2, com os dados de demonstração)
// e o frontend compilado, e usam o sistema pelo navegador como o usuário. Antes, gere o jar do backend:
// na pasta backend, ./mvnw -DskipTests package
const FRONTEND = "http://localhost:4173";

export default defineConfig({
  testDir: "tests/e2e",
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never", outputFolder: "playwright-report" }], ["junit", { outputFile: "playwright-report/resultados.xml" }]],
  use: {
    baseURL: FRONTEND,
    locale: "pt-BR",
    timezoneId: "America/Recife",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: [
    {
      command: "java -jar ../backend/target/lanchonete-api-0.0.1-SNAPSHOT.jar",
      url: "http://localhost:8080/api/publico/identidade",
      env: { CORS_ORIGENS: FRONTEND },
      timeout: 240_000,
      reuseExistingServer: !process.env.CI,
    },
    {
      command: "npm run build && npm run preview -- --port 4173 --strictPort",
      url: FRONTEND,
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
});
