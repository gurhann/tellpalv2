import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { RouterProvider, createMemoryRouter } from "react-router-dom";
import { describe, expect, it } from "vitest";

import { MockupCategoryDetailRoute } from "@/app/routes/mockups/category-detail";
import { MockupCategoriesRoute } from "@/app/routes/mockups/categories";
import { MockupContentDetailRoute } from "@/app/routes/mockups/content-detail";
import { MockupContentsRoute } from "@/app/routes/mockups/contents";
import { MockupStoryPagesRoute } from "@/app/routes/mockups/story-pages";
import { LocaleProvider } from "@/i18n/locale-provider";

const mockupRoutes = [
  {
    path: "/labs/mockups/contents",
    element: <MockupContentsRoute />,
  },
  {
    path: "/labs/mockups/contents/demo-content",
    element: <MockupContentDetailRoute />,
  },
  {
    path: "/labs/mockups/contents/demo-content/story-pages",
    element: <MockupStoryPagesRoute />,
  },
  {
    path: "/labs/mockups/categories",
    element: <MockupCategoriesRoute />,
  },
  {
    path: "/labs/mockups/categories/demo-category",
    element: <MockupCategoryDetailRoute />,
  },
];

function renderMockupRoute(initialEntry: string) {
  const router = createMemoryRouter(mockupRoutes, {
    initialEntries: [initialEntry],
  });

  return render(
    <LocaleProvider>
      <RouterProvider router={router} />
    </LocaleProvider>,
  );
}

describe("Variant A mockup routes", () => {
  it("opens and closes the content create modal", async () => {
    renderMockupRoute("/labs/mockups/contents");

    fireEvent.click(
      await screen.findByRole("button", { name: /create content/i }),
    );

    const dialog = await screen.findByRole("dialog", {
      name: /create content/i,
    });
    expect(dialog).toBeInTheDocument();

    fireEvent.click(within(dialog).getAllByRole("button", { name: /^close$/i })[0]);

    await waitFor(() => {
      expect(
        screen.queryByRole("dialog", { name: /create content/i }),
      ).not.toBeInTheDocument();
    });
  });

  it("preserves the selected locale when navigating from content detail to story pages", async () => {
    renderMockupRoute("/labs/mockups/contents/demo-content");

    const turkishTab = await screen.findByRole("tab", { name: /turkish/i });
    fireEvent.click(turkishTab);

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: /turkish/i })).toHaveAttribute(
        "data-state",
        "active",
      );
    });

    fireEvent.click(
      screen.getAllByRole("link", { name: /open story page mockup/i })[0],
    );

    expect(
      await screen.findByRole("heading", {
        name: /story page editor mockup/i,
      }),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/preferred locale/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/turkish/i).length).toBeGreaterThan(0);
  });

  it("changes the selected locale in the category detail workspace", async () => {
    renderMockupRoute("/labs/mockups/categories/demo-category");

    const turkishTab = await screen.findByRole("tab", { name: /turkish/i });
    fireEvent.click(turkishTab);

    await waitFor(() => {
      expect(screen.getByRole("tab", { name: /turkish/i })).toHaveAttribute(
        "data-state",
        "active",
      );
    });
    expect(screen.getByRole("heading", { name: /sakin liman seckileri/i })).toBeInTheDocument();
  });

  it("opens the story page editor modal and switches locale tabs inside the modal", async () => {
    renderMockupRoute("/labs/mockups/contents/demo-content/story-pages?language=tr");

    fireEvent.click((await screen.findAllByRole("button", { name: /edit page/i }))[0]);

    const dialog = await screen.findByRole("dialog", { name: /edit story page #1/i });
    const turkishTab = within(dialog).getByRole("tab", { name: /turkish/i });

    expect(turkishTab).toHaveAttribute("data-state", "active");

    const englishTab = within(dialog).getByRole("tab", { name: /english/i });
    fireEvent.click(englishTab);

    await waitFor(() => {
      expect(
        within(dialog).getByRole("tab", { name: /english/i }),
      ).toHaveAttribute("data-state", "active");
    });
  });
});
