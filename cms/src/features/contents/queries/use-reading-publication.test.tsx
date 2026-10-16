import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { contentAdminApi, type ReadingPublicationStatus } from "@/features/contents/api/content-admin";
import { queryKeys } from "@/lib/query-keys";
import { readingPublicationPollInterval, useReadingPublication } from "./use-reading-publication";
const status: ReadingPublicationStatus = { enabled: true, contract: "INTERNAL_LOCAL_TEST", state: "PENDING", generation: 1, liveRevision: null, diagnostic: null, retryable: false };
afterEach(() => vi.restoreAllMocks());
describe("reading publication query", () => {
  it("polls pending states only and stops on terminal or network failure", () => {
    expect(readingPublicationPollInterval("PENDING")).toBe(2000);
    expect(readingPublicationPollInterval("PREPARING")).toBe(2000);
    for (const state of ["DISABLED", "NOT_REQUESTED", "LIVE", "FAILED", "WITHDRAWN"]) expect(readingPublicationPollInterval(state)).toBe(false);
    expect(readingPublicationPollInterval("PENDING", true)).toBe(false);
  });
  it("isolates language keys, cancels obsolete requests and never retries mutations automatically", async () => {
    let previous: AbortSignal | undefined;
    const get = vi.spyOn(contentAdminApi, "getReadingPublication").mockImplementation((_id, lang, signal) => {
      if (lang === "en") { previous = signal; return new Promise(() => {}); }
      return Promise.resolve(status);
    });
    const retry = vi.spyOn(contentAdminApi, "retryReadingPublication").mockRejectedValue(new Error("offline"));
    const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: 5 } } });
    const wrapper = ({ children }: PropsWithChildren) => <QueryClientProvider client={client}>{children}</QueryClientProvider>;
    const { result, rerender, unmount } = renderHook(({ language }) => useReadingPublication(1, language, true), { wrapper, initialProps: { language: "en" } });
    await waitFor(() => expect(get).toHaveBeenCalled());rerender({ language: "tr" });
    await waitFor(() => expect(result.current.data?.state).toBe("PENDING"));expect(previous?.aborted).toBe(true);
    expect(client.getQueryData(queryKeys.contents.readingPublication(1, "en"))).toBeUndefined();
    await act(async () => { await expect(result.current.retry.mutateAsync()).rejects.toThrow("offline"); });
    expect(retry).toHaveBeenCalledTimes(1);unmount();client.clear();
  });
});
