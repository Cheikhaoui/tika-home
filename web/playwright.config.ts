import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests in a real browser. They need the whole stack:
 *   docker compose up -d                      (PostgreSQL + Mailpit)
 *   backend on :8081                          (started below if not already running; needs JDK 25)
 *   Next.js on :3000                          (started below if not already running)
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? "github" : "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["Pixel 7"] } },
  ],
  webServer: [
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
