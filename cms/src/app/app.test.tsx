import { render, screen } from "@testing-library/react";

import { AppProviders } from "@/app/providers";
import App from "@/App";

describe("App shell bootstrap", () => {
  it("renders the login route by default", async () => {
    window.history.replaceState({}, "", "/login");

    render(
      <AppProviders>
        <App />
      </AppProviders>,
    );

    expect(
      await screen.findByRole("heading", { name: /login route is ready/i }),
    ).toBeInTheDocument();
  });
});
