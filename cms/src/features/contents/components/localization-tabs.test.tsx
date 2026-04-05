import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { ContentLocalizationTabs } from "./localization-tabs";

const contentLocalizationActionsMock = vi.hoisted(() => ({
  useContentLocalizationActions: vi.fn(),
}));
const assetDetailHookMock = vi.hoisted(() => ({
  useAssetDetail: vi.fn(),
}));

vi.mock(
  "@/features/contents/mutations/use-content-localization-actions",
  () => ({
    useContentLocalizationActions:
      contentLocalizationActionsMock.useContentLocalizationActions,
  }),
);

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: assetDetailHookMock.useAssetDetail,
}));

function makeMutationState(overrides: Record<string, unknown> = {}) {
  return {
    mutateAsync: vi.fn(),
    reset: vi.fn(),
    isPending: false,
    error: null,
    ...overrides,
  };
}

beforeEach(() => {
  contentLocalizationActionsMock.useContentLocalizationActions.mockReset();
  assetDetailHookMock.useAssetDetail.mockReset();
  contentLocalizationActionsMock.useContentLocalizationActions.mockReturnValue({
    saveLocalization: makeMutationState(),
    publishLocalization: makeMutationState(),
    archiveLocalization: makeMutationState(),
  });
  assetDetailHookMock.useAssetDetail.mockReturnValue({
    asset: null,
    isLoading: false,
    problem: null,
    isNotFound: false,
  });
});

describe("ContentLocalizationTabs", () => {
  it("reports the initial active localization language to parent shells", () => {
    const handleActiveLanguageChange = vi.fn();

    render(
      <ContentLocalizationTabs
        content={storyContentViewModel}
        onActiveLanguageChange={handleActiveLanguageChange}
      />,
    );

    expect(handleActiveLanguageChange).toHaveBeenCalledWith("en");
  });

  it("renders live language tabs, editor forms, and publication controls", () => {
    render(<ContentLocalizationTabs content={storyContentViewModel} />);

    expect(
      screen.getByRole("tablist", { name: /content localization tabs/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: /add localization/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: /save localization/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: /publish locale/i }),
    ).toBeVisible();
    expect(screen.getByRole("tab", { name: /turkish/i })).toBeInTheDocument();
  });

  it("shows an empty-state workflow when the content has no localizations", () => {
    render(<ContentLocalizationTabs content={inactiveContentViewModel} />);

    expect(screen.getByText(/no localizations yet/i)).toBeVisible();
    expect(
      screen.getByRole("button", { name: /create first localization/i }),
    ).toBeVisible();
  });

  it("opens the create-localization dialog from the empty state", () => {
    render(<ContentLocalizationTabs content={inactiveContentViewModel} />);

    fireEvent.click(
      screen.getByRole("button", { name: /create first localization/i }),
    );

    expect(
      screen.getByRole("heading", { name: /create localization/i }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: /create localization/i }),
    ).toBeVisible();
  });
});
