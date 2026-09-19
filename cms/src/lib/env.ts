import { z } from "zod";

const envSchema = z.object({
  VITE_APP_TITLE: z.string().trim().min(1).default("TellPal CMS"),
  VITE_API_BASE_URL: z.url().default("http://localhost:8080"),
});

export const appEnv = envSchema.parse(import.meta.env);

export type AppEnv = typeof appEnv;
