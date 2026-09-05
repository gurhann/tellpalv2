import { describe, expect, it } from "vitest";

import type { ApiProblemDetail } from "@/types/api";

import { localizeContributorProblem } from "./contributor-problems";

const problem: ApiProblemDetail = {
  type: "about:blank",
  title: "Backend title",
  status: 400,
  detail: "Backend detail",
  errorCode: "contributor_role_not_supported",
};

describe("localizeContributorProblem", () => {
  it("resolves known contributor errors through the active locale", () => {
    const localized = localizeContributorProblem(problem, (key) =>
      key === "contributors.errors.roleNotSupportedTitle"
        ? "Rol kullanılamıyor"
        : "Seçilen rol desteklenmiyor",
    );

    expect(localized.title).toBe("Rol kullanılamıyor");
    expect(localized.detail).toBe("Seçilen rol desteklenmiyor");
  });

  it("keeps unknown problem details as the generic fallback", () => {
    const unknown = { ...problem, errorCode: "new_backend_error" };
    expect(localizeContributorProblem(unknown, () => "localized")).toEqual(
      unknown,
    );
  });
});
