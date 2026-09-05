import { getProblemMessage } from "@/lib/http/problem-details";
import type { ApiProblemDetail } from "@/types/api";

type Translate = (
  key: never,
  params?: Record<string, string | number>,
) => string;

/** Resolves contributor assignment problem codes without exposing backend English detail text. */
export function localizeContributorProblem(
  problem: ApiProblemDetail,
  translate: Translate,
): ApiProblemDetail {
  const key = (() => {
    switch (problem.errorCode) {
      case "contributor_role_not_supported":
        return [
          "contributors.errors.roleNotSupportedTitle",
          "contributors.errors.roleNotSupported",
        ];
      case "content_contributor_language_not_found":
        return [
          "contributors.errors.languageNotFoundTitle",
          "contributors.errors.languageNotFound",
        ];
      case "content_contributor_assignment_exists":
        return [
          "contributors.errors.assignmentExistsTitle",
          "contributors.errors.assignmentExists",
        ];
      case "duplicate_contributor_display_name":
        return [
          "contributors.errors.duplicateNameTitle",
          "contributors.errors.duplicateName",
        ];
      default:
        return null;
    }
  })();

  if (!key) return problem;

  return {
    ...problem,
    title: translate(key[0] as never),
    detail: translate(key[1] as never),
  };
}

export function getLocalizedContributorProblemMessage(
  problem: ApiProblemDetail,
  translate: Translate,
) {
  return getProblemMessage(localizeContributorProblem(problem, translate));
}
