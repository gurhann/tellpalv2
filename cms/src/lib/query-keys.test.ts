import { compactQueryKeyParams, queryKeys } from "@/lib/query-keys";

describe("query key helpers", () => {
  it("removes empty string filters and keeps a stable key order", () => {
    expect(
      compactQueryKeyParams({
        type: "STORY",
        search: "   ",
        active: true,
        limit: 20,
      }),
    ).toEqual({
      active: true,
      limit: 20,
      type: "STORY",
    });
  });

  it("normalizes language-based query keys", () => {
    expect(queryKeys.contents.localization(42, " EN ")).toEqual([
      "contents",
      "detail",
      42,
      "localizations",
      "en",
    ]);

    expect(queryKeys.assets.processingStatus(17, "TR")).toEqual([
      "assets",
      "processing",
      "status",
      17,
      "tr",
    ]);
  });
});
