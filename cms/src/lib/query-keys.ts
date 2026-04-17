import { normalizeLanguageCode } from "@/lib/languages";

type QueryKeyPrimitive = string | number | boolean | null;
type QueryKeyParams = Record<string, QueryKeyPrimitive | undefined>;

function normalizeQueryParamValue(value: QueryKeyPrimitive | undefined) {
  if (typeof value === "string") {
    const trimmed = value.trim();
    return trimmed.length > 0 ? trimmed : undefined;
  }

  return value === undefined ? undefined : value;
}

export function compactQueryKeyParams<TParams extends QueryKeyParams>(
  params?: TParams,
) {
  const entries = Object.entries(params ?? {})
    .map(([key, value]) => [key, normalizeQueryParamValue(value)] as const)
    .filter(
      (entry): entry is readonly [string, QueryKeyPrimitive] =>
        entry[1] !== undefined,
    )
    .sort(([left], [right]) => left.localeCompare(right));

  return Object.fromEntries(entries) as Record<string, QueryKeyPrimitive>;
}

export const queryKeys = {
  auth: {
    all: ["auth"] as const,
    session: () => ["auth", "session"] as const,
  },
  contents: {
    all: ["contents"] as const,
    lists: () => ["contents", "list"] as const,
    list: (params?: QueryKeyParams) =>
      ["contents", "list", compactQueryKeyParams(params)] as const,
    details: () => ["contents", "detail"] as const,
    detail: (contentId: number) => ["contents", "detail", contentId] as const,
    localizations: (contentId: number) =>
      ["contents", "detail", contentId, "localizations"] as const,
    localization: (contentId: number, languageCode: string) =>
      [
        "contents",
        "detail",
        contentId,
        "localizations",
        normalizeLanguageCode(languageCode),
      ] as const,
    storyPages: (contentId: number) =>
      ["contents", "detail", contentId, "story-pages"] as const,
    storyPage: (contentId: number, pageNumber: number) =>
      ["contents", "detail", contentId, "story-pages", pageNumber] as const,
    storyPageLocalization: (
      contentId: number,
      pageNumber: number,
      languageCode: string,
    ) =>
      [
        "contents",
        "detail",
        contentId,
        "story-pages",
        pageNumber,
        "localizations",
        normalizeLanguageCode(languageCode),
      ] as const,
    contributors: (contentId: number) =>
      ["contents", "detail", contentId, "contributors"] as const,
  },
  categories: {
    all: ["categories"] as const,
    lists: () => ["categories", "list"] as const,
    list: (params?: QueryKeyParams) =>
      ["categories", "list", compactQueryKeyParams(params)] as const,
    details: () => ["categories", "detail"] as const,
    detail: (categoryId: number) =>
      ["categories", "detail", categoryId] as const,
    localizations: (categoryId: number) =>
      ["categories", "detail", categoryId, "localizations"] as const,
    localization: (categoryId: number, languageCode: string) =>
      [
        "categories",
        "detail",
        categoryId,
        "localizations",
        normalizeLanguageCode(languageCode),
      ] as const,
    curation: (categoryId: number, languageCode: string) =>
      [
        "categories",
        "detail",
        categoryId,
        "localizations",
        normalizeLanguageCode(languageCode),
        "curation",
      ] as const,
    eligibleContentsRoot: (categoryId: number, languageCode: string) =>
      [
        "categories",
        "detail",
        categoryId,
        "localizations",
        normalizeLanguageCode(languageCode),
        "eligible-contents",
      ] as const,
    eligibleContents: (
      categoryId: number,
      languageCode: string,
      params?: QueryKeyParams,
    ) =>
      [
        "categories",
        "detail",
        categoryId,
        "localizations",
        normalizeLanguageCode(languageCode),
        "eligible-contents",
        compactQueryKeyParams(params),
      ] as const,
  },
  assets: {
    all: ["assets"] as const,
    recent: (params?: QueryKeyParams) =>
      ["assets", "recent", compactQueryKeyParams(params)] as const,
    detail: (assetId: number) => ["assets", "detail", assetId] as const,
    processingRecentRoot: () => ["assets", "processing", "recent"] as const,
    processingRecent: (params?: QueryKeyParams) =>
      [
        "assets",
        "processing",
        "recent",
        compactQueryKeyParams(params),
      ] as const,
    processingStatus: (contentId: number, languageCode: string) =>
      [
        "assets",
        "processing",
        "status",
        contentId,
        normalizeLanguageCode(languageCode),
      ] as const,
  },
  contributors: {
    all: ["contributors"] as const,
    lists: () => ["contributors", "list"] as const,
    list: (params?: QueryKeyParams) =>
      ["contributors", "list", compactQueryKeyParams(params)] as const,
    detail: (contributorId: number) =>
      ["contributors", "detail", contributorId] as const,
    assignments: (contentId: number) =>
      ["contributors", "assignments", contentId] as const,
  },
  freeAccess: {
    all: ["free-access"] as const,
    lists: () => ["free-access", "list"] as const,
    list: (params?: QueryKeyParams) =>
      ["free-access", "list", compactQueryKeyParams(params)] as const,
  },
} as const;
