import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { PropsWithChildren } from "react";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { queryKeys } from "@/lib/query-keys";
import {
  contentContributorViewModels,
  globalContentContributorViewModel,
} from "@/features/contributors/test/fixtures";
import {
  inactiveContentViewModel,
  storyContentViewModel,
} from "@/features/contents/test/fixtures";

import { ContentContributorPanel } from "./content-contributor-panel";

function createWrapper(seed?: { queryKey: readonly unknown[]; data: unknown }) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  if (seed) {
    queryClient.setQueryData(seed.queryKey, seed.data);
  }

  return function Wrapper({ children }: PropsWithChildren) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{children}</MemoryRouter>
      </QueryClientProvider>
    );
  };
}

describe("ContentContributorPanel", () => {
  it("renders current-session contributor assignments from the cache", () => {
    render(<ContentContributorPanel content={storyContentViewModel} />, {
      wrapper: createWrapper({
        queryKey: queryKeys.contributors.assignments(1),
        data: [
          globalContentContributorViewModel,
          ...contentContributorViewModels,
        ],
      }),
    });

    expect(
      screen.getAllByText(/current-session assignments?/i)[0],
    ).toBeVisible();
    expect(screen.getAllByText("Annie Case")).toHaveLength(2);
    expect(screen.getByText("M. Rivers")).toBeVisible();
    expect(screen.getByText("Author")).toBeVisible();
    expect(screen.getByText("Turkish")).toBeVisible();
    expect(screen.getByText("All languages")).toBeVisible();
    expect(
      screen.getByText(/unassign contributor unavailable until bg03/i),
    ).toBeVisible();
  });

  it("keeps assignment enabled when the content has no localizations", () => {
    render(<ContentContributorPanel content={inactiveContentViewModel} />, {
      wrapper: createWrapper(),
    });

    expect(
      screen.getByRole("button", { name: /assign contributor/i }),
    ).toBeEnabled();
    expect(
      screen.getByText(
        /global or localized author, illustrator, narrator, or musician credits/i,
      ),
    ).toBeVisible();
  });
});
