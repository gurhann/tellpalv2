import { defineConfig } from "@playwright/test";
import base from "./playwright.config";

export default defineConfig({
  ...base,
  testMatch: "reading-publication-local.spec.ts",
  fullyParallel: false,
  retries: 0,
  use: { ...base.use, baseURL: "http://127.0.0.1:4177", trace: "off" },
  outputDir: process.env.TELLPAL_CMS_ACCEPTANCE_DIRECTORY
    ? `${process.env.TELLPAL_CMS_ACCEPTANCE_DIRECTORY}/results`
    : "/private/tmp/tellpal-cms-publication-results",
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 4177 --strictPort",
    url: "http://127.0.0.1:4177",
    reuseExistingServer: false,
    timeout: 60_000,
  },
});
