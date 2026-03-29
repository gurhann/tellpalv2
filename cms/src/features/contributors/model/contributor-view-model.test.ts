import type {
  AdminContentContributorResponse,
  AdminContributorResponse,
} from "@/features/contributors/api/contributor-admin";
import {
  mapAdminContentContributor,
  mapAdminContributor,
} from "@/features/contributors/model/contributor-view-model";

describe("contributor view model mappers", () => {
  it("maps contributor summaries with initials", () => {
    const dto: AdminContributorResponse = {
      contributorId: 22,
      displayName: "Annie Case",
    };

    expect(mapAdminContributor(dto)).toEqual({
      id: 22,
      displayName: "Annie Case",
      initials: "AC",
    });
  });

  it("maps content contributor relations into display-friendly data", () => {
    const dto: AdminContentContributorResponse = {
      contentId: 14,
      contributorId: 22,
      contributorDisplayName: "Annie Case",
      role: "NARRATOR",
      languageCode: "EN",
      creditName: null,
      sortOrder: 1,
    };

    expect(mapAdminContentContributor(dto)).toEqual({
      contentId: 14,
      contributorId: 22,
      displayName: "Annie Case",
      initials: "AC",
      role: "NARRATOR",
      roleLabel: "Narrator",
      languageCode: "en",
      languageLabel: "English",
      creditName: null,
      effectiveCreditName: "Annie Case",
      sortOrder: 1,
    });
  });
});
