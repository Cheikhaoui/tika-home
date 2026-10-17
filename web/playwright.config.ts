import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests in a real browser, against either:
 * - the dev stack (default): docker compose up -d, then the backend (:8081) and Next.js (:3000) are
 *   started below if not already running;
 * - an already running stack, e.g. the production containers:
 *   E2E_BASE_URL=http://localhost:3001 npm run e2e   (see docker-compose.full.yml)
 */
const externalBaseUrl = process.env.E2E_BASE_URL;
export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: externalBaseUrl ?? "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  webServer: externalBaseUrl ? undefined : [
    {
      command: "../backend/mvnw -q -f ../backend/pom.xml spring-boot:run",
      url: "http://localhost:8081/v3/api-docs",
      reuseExistingServer: true,
      timeout: 180_000,
    },
    {
      command: "npm run dev",
      url: "http://localhost:3000/fr",
      reuseExistingServer: true,
      timeout: 120_000,
    },
  ],
});
