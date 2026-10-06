import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { CategoryHomeRoute } from "./home";
import {
  categoryHomeAdminApi,
  type HomeSnapshot,
} from "@/features/categories/api/category-home-admin";

vi.mock("@/i18n/locale-provider", () => ({
  useI18n: () => ({ locale: "en" }),
}));
vi.mock("@/features/categories/api/category-home-admin", () => ({
  categoryHomeAdminApi: { read: vi.fn(), save: vi.fn() },
}));
const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value));
const snapshot = (
  language: string,
  revision = `${language}-1`,
): HomeSnapshot => ({
  language,
  revision,
  rows: [1, 2].map((categoryId) => ({
    categoryId,
    slug: `row-${categoryId}`,
    name: `${language} Row ${categoryId}`,
    description: null,
    type: "STORY",
    active: true,
    status: "PUBLISHED",
    version: 0,
    displayOrder: categoryId - 1,
    audience: "ALL",
    contentLanguage: null,
  })),
});
let stored: Record<string, HomeSnapshot>;
let client: QueryClient;
const firstRow = () =>
  within(screen.getByRole("list", { name: "Library row order" })).getAllByRole(
    "listitem",
  )[0];
const audience = () =>
  screen.getByRole("combobox", { name: "tr Row 2 audience" });
const contentLanguage = () =>
  screen.getByRole("combobox", { name: "tr Row 2 content language" });
const edit = () => {
  fireEvent.click(screen.getByRole("button", { name: "Move up: tr Row 2" }));
  fireEvent.change(audience(), { target: { value: "NON_SUBSCRIBER" } });
  fireEvent.change(contentLanguage(), { target: { value: "en" } });
};
const assertDraft = () => {
  expect(firstRow()).toHaveTextContent("tr Row 2");
  expect(audience()).toHaveValue("NON_SUBSCRIBER");
  expect(contentLanguage()).toHaveValue("en");
  expect(
    screen.getByRole("combobox", { name: "Presentation language" }),
  ).toHaveValue("tr");
};
const mount = async () => {
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={["/categories/home?lang=tr"]}>
        <CategoryHomeRoute />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  await screen.findByRole("list", { name: "Library row order" });
};
const refetch = async () => {
  await act(async () => {
    await client.refetchQueries({
      queryKey: ["categories", "library-home", "tr"],
    });
  });
};
beforeEach(() => {
  stored = { tr: snapshot("tr"), en: snapshot("en") };
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  vi.mocked(categoryHomeAdminApi.read).mockImplementation(async (language) =>
    clone(stored[language]),
  );
  vi.mocked(categoryHomeAdminApi.save).mockImplementation(
    async (language, expectedRevision, rows) => {
      expect(expectedRevision).toBe(stored[language].revision);
      stored[language] = {
        ...stored[language],
        revision: `${expectedRevision}-saved`,
        rows: stored[language].rows.map((row) => ({
          ...row,
          ...rows.find((input) => input.categoryId === row.categoryId),
        })),
      };
      return clone(stored[language]);
    },
  );
});
afterEach(() => {
  client.clear();
  vi.resetAllMocks();
});

describe("pinned Library editing snapshot", () => {
  it("keeps draft and original expectedRevision across background GET failure", async () => {
    await mount();
    edit();
    vi.mocked(categoryHomeAdminApi.read).mockRejectedValueOnce(
      new Error("Background network failure"),
    );
    await refetch();
    expect(await screen.findByText("Order unavailable")).toBeVisible();
    assertDraft();
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    expect(categoryHomeAdminApi.save).toHaveBeenCalledWith(
      "tr",
      "tr-1",
      expect.arrayContaining([
        expect.objectContaining({
          categoryId: 2,
          displayOrder: 0,
          audience: "NON_SUBSCRIBER",
          contentLanguage: "en",
        }),
      ]),
    );
  });
  it("keeps stale draft revision on changed-server background data and failed explicit Reload", async () => {
    await mount();
    edit();
    stored.tr = snapshot("tr", "tr-2");
    await refetch();
    expect(await screen.findByText("Library order changed")).toBeVisible();
    assertDraft();
    vi.mocked(categoryHomeAdminApi.save).mockRejectedValueOnce(
      new Error("Conflict"),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Save failed");
    expect(categoryHomeAdminApi.save).toHaveBeenCalledWith(
      "tr",
      "tr-1",
      expect.any(Array),
    );
    vi.mocked(categoryHomeAdminApi.read).mockRejectedValueOnce(
      new Error("Reload failure"),
    );
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await screen.findByText("Order unavailable");
    assertDraft();
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await waitFor(() =>
      expect(categoryHomeAdminApi.save).toHaveBeenCalledTimes(2),
    );
    expect(categoryHomeAdminApi.save).toHaveBeenLastCalledWith(
      "tr",
      "tr-1",
      expect.any(Array),
    );
    await screen.findByText("Save failed");
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() => expect(firstRow()).toHaveTextContent("tr Row 1"));
    expect(audience()).toHaveValue("ALL");
    expect(contentLanguage()).toHaveValue("");
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    expect(categoryHomeAdminApi.save).toHaveBeenLastCalledWith(
      "tr",
      "tr-2",
      expect.any(Array),
    );
  });
  it("successful Save pins all edited fields; successful Reload and language change replace snapshot", async () => {
    await mount();
    edit();
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    assertDraft();
    expect(stored.tr.rows.find((row) => row.categoryId === 2)).toMatchObject({
      displayOrder: 0,
      audience: "NON_SUBSCRIBER",
      contentLanguage: "en",
    });
    expect(stored.en).toEqual(snapshot("en"));
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() =>
      expect(screen.queryByText("Order saved.")).not.toBeInTheDocument(),
    );
    assertDraft();
    fireEvent.change(
      screen.getByRole("combobox", { name: "Presentation language" }),
      { target: { value: "en" } },
    );
    await waitFor(() => expect(firstRow()).toHaveTextContent("en Row 1"));
    fireEvent.change(
      screen.getByRole("combobox", { name: "Presentation language" }),
      { target: { value: "tr" } },
    );
    await waitFor(assertDraft);
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    expect(categoryHomeAdminApi.save).toHaveBeenLastCalledWith(
      "tr",
      "tr-1-saved",
      expect.any(Array),
    );
  });
  it("opts rows out and in across save/reload without overflowing signed integer positions", async () => {
    stored.tr.rows[0].displayOrder = 2_147_483_647;
    stored.tr.rows[1].displayOrder = null;
    await mount();
    const checkbox = () =>
      screen.getByRole("checkbox", { name: "tr Row 2 Show in Library" });
    expect(checkbox()).not.toBeChecked();
    fireEvent.click(checkbox());
    expect(checkbox()).toBeChecked();
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    expect(stored.tr.rows.map((row) => row.displayOrder)).toEqual([0, 1]);
    expect(stored.en).toEqual(snapshot("en"));
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() =>
      expect(screen.queryByText("Order saved.")).not.toBeInTheDocument(),
    );
    expect(checkbox()).toBeChecked();
    fireEvent.click(checkbox());
    fireEvent.click(screen.getByRole("button", { name: "Save order" }));
    await screen.findByText("Order saved.");
    expect(stored.tr.rows[1].displayOrder).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Reload" }));
    await waitFor(() =>
      expect(screen.queryByText("Order saved.")).not.toBeInTheDocument(),
    );
    expect(checkbox()).not.toBeChecked();
  });
});
