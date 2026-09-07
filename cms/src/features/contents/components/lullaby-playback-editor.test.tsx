import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { inactiveContentViewModel } from "@/features/contents/test/fixtures";

import { LullabyPlaybackEditor } from "./lullaby-playback-editor";

const apiMock = vi.hoisted(() => ({
  listLullabyInstruments: vi.fn(),
  listInstrumentCatalog: vi.fn(),
  updateLullabyPlayback: vi.fn(),
  replaceLullabyInstruments: vi.fn(),
}));

vi.mock("@/features/contents/api/content-admin", async () => {
  const actual = await vi.importActual<typeof import("@/features/contents/api/content-admin")>(
    "@/features/contents/api/content-admin",
  );
  return { ...actual, contentAdminApi: { ...actual.contentAdminApi, ...apiMock } };
});

vi.mock("@/features/assets/queries/use-asset-detail", () => ({
  useAssetDetail: () => ({ asset: null, isLoading: false, problem: null }),
}));
vi.mock("@/features/assets/mutations/use-upload-asset", () => ({
  useUploadAsset: () => ({ mutateAsync: vi.fn(), isPending: false, problem: null, reset: vi.fn() }),
}));

function wrapper({ children }: PropsWithChildren) {
  return <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>;
}

beforeEach(() => {
  apiMock.listLullabyInstruments.mockResolvedValue([{ instrumentId: 1, code: "PIANO", displayName: "Piano", displayOrder: 0 }]);
  apiMock.listInstrumentCatalog.mockResolvedValue([
    { instrumentId: 1, code: "PIANO", displayName: "Piano" },
    { instrumentId: 2, code: "HARP", displayName: "Harp" },
  ]);
  apiMock.updateLullabyPlayback.mockResolvedValue({ contentId: 4, audioMediaId: 44, durationMinutes: 3, processingStatus: "PENDING", processingError: null });
  apiMock.replaceLullabyInstruments.mockResolvedValue([]);
});

describe("LullabyPlaybackEditor", () => {
  it("saves one content-level playback and an ordered stable-code instrument selection", async () => {
    const content = {
      ...inactiveContentViewModel,
      playback: { audioAssetId: 44, durationMinutes: 3, processingStatus: "COMPLETED" as const, processingError: null, instruments: [] },
    };
    render(<LullabyPlaybackEditor content={content} languageCode="en" />, { wrapper });

    expect(await screen.findByText("Piano")).toBeVisible();
    fireEvent.click(screen.getByRole("button", { name: "Harp" }));
    fireEvent.click(screen.getByRole("button", { name: "Save instrument order" }));
    await waitFor(() => expect(apiMock.replaceLullabyInstruments).toHaveBeenCalledWith(4, ["PIANO", "HARP"], "en"));

    fireEvent.click(screen.getByRole("button", { name: "Save shared playback" }));
    await waitFor(() => expect(apiMock.updateLullabyPlayback).toHaveBeenCalledWith(4, { audioMediaId: 44, durationMinutes: 3 }));
    expect(screen.getByText(/Processing: COMPLETED/)).toBeVisible();
  });

  it("keeps the workspace usable when the catalog fails to load", async () => {
    apiMock.listInstrumentCatalog.mockRejectedValueOnce(new Error("catalog unavailable"));
    render(<LullabyPlaybackEditor content={inactiveContentViewModel} languageCode="en" />, { wrapper });
    expect(await screen.findByRole("alert")).toHaveTextContent(/catalog could not be loaded/i);
    expect(screen.getByRole("button", { name: "Save shared playback" })).toBeEnabled();
  });
});
