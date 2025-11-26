import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { AuthProvider } from "./auth/AuthContext";
import { appConfig } from "./config/env";
import { routes } from "./routes";

function renderWithProviders(router: ReturnType<typeof createMemoryRouter>) {
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
}

describe("App routing", () => {
  it("renders the home page by default", () => {
    const router = createMemoryRouter(routes, { initialEntries: ["/"] });
    renderWithProviders(router);

    expect(screen.getByRole("heading", { name: /city rides made simple/i })).toBeInTheDocument();
  });

  it("renders the not found page for unknown routes", () => {
    const router = createMemoryRouter(routes, { initialEntries: ["/missing"] });
    renderWithProviders(router);

    expect(
      screen.getByRole("heading", { name: /404 - not found/i }),
    ).toBeInTheDocument();
  });
});

describe("App configuration", () => {
  it("exposes an API base URL", () => {
    expect(appConfig.apiUrl).toBe("http://localhost:8080/api");
  });
});
