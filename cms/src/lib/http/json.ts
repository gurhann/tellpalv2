import type { ZodType } from "zod";

export class JsonSchemaValidationError extends Error {
  readonly issues: string[];

  constructor(message: string, issues: string[]) {
    super(message);
    this.name = "JsonSchemaValidationError";
    this.issues = issues;
  }
}

export async function readJsonBody(
  response: Response,
): Promise<unknown | null> {
  if (response.status === 204 || response.status === 205) {
    return null;
  }

  const text = await response.text();

  if (!text.trim()) {
    return null;
  }

  try {
    return JSON.parse(text) as unknown;
  } catch (error) {
    throw new Error(
      `Response from ${response.url} did not contain valid JSON.`,
      {
        cause: error,
      },
    );
  }
}

export function parseJsonWithSchema<T>(
  input: unknown,
  schema: ZodType<T>,
  source: string,
): T {
  const result = schema.safeParse(input);

  if (result.success) {
    return result.data;
  }

  const issues = result.error.issues.map((issue) => {
    const path = issue.path.length > 0 ? issue.path.join(".") : "root";
    return `${path}: ${issue.message}`;
  });

  throw new JsonSchemaValidationError(
    `${source} did not match the expected schema.`,
    issues,
  );
}
