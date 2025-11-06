import { describe, expect, it, beforeEach, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { routes } from "../../routes";
import { AuthProvider } from "../../auth/AuthContext";

vi.mock("../../api/client", () => {
  return {
    apiRequest: vi.fn(async (path: string, opts?: RequestInit) => {
      if (path === "/auth/login" && opts?.method === "POST") {
        return {
          token: "demo-token",
          role: "RIDER",
          userId: "u1",
          username: "rider1",
        };
      }
      if (path === "/pricing") {
        return [
          {
            planId: "p1",
            name: "Pay As You Go",
            description: "Pay for minutes only.",
            planType: "PAY_AS_YOU_GO",
            baseCost: 0,
            perMinuteRate: 0.05,
            eBikeSurchargePerMinute: 0.01,
            subscriptionFee: null,
            sample: {
              durationMinutes: 30,
              standardBikeCost: 1.5,
              eBikeCost: 1.8,
            },
          },
        ];
      }
      if (path === "/stations") {
        return [];
      }
      if (path === "/public/events") {
        return [];
      }
      if (path.startsWith("/stations/") && path.endsWith("/details")) {
        return {
          stationId: "s1",
          name: "Station #1",
          status: "OCCUPIED",
          bikesAvailable: 2,
          bikesDocked: 3,
          capacity: 5,
          freeDocks: 2,
          latitude: 45.5,
          longitude: -73.6,
          fullnessCategory: "HEALTHY",
          docks: [
            { dockId: "d1", status: "OCCUPIED", bikeId: "b1" },
            { dockId: "d2", status: "EMPTY", bikeId: null },
          ],
        };
      }
      return undefined;
    }),
  };
});

function renderWithProviders(initialEntries: string[]) {
  const router = createMemoryRouter(routes, { initialEntries });
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
}

describe("Auth guard", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("shows login page when accessing dashboard without token", () => {
    renderWithProviders(["/dashboard"]);
    expect(
      screen.getByRole("heading", { name: /sign in/i }),
    ).toBeInTheDocument();
  });

  it("renders dashboard when token exists", async () => {
    localStorage.setItem(
      "sharecycle.auth",
      JSON.stringify({
        token: "t",
        role: "RIDER",
        userId: "u1",
        username: "r",
      }),
    );
    renderWithProviders(["/dashboard"]);
    expect(
      await screen.findByRole("heading", { name: /Station Overview/i }),
    ).toBeInTheDocument();
  });
});

describe("Login flow", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("logs in and navigates to dashboard", async () => {
    const router = createMemoryRouter(routes, { initialEntries: ["/login"] });
    render(
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>,
    );
    fireEvent.change(screen.getByLabelText(/Username/i), {
      target: { value: "rider1" },
    });
    fireEvent.change(screen.getByLabelText(/Password/i), {
      target: { value: "password123" },
    });
    fireEvent.click(screen.getByRole("button", { name: /login/i }));
    // After login, router should navigate to dashboard and render Station Overview
    expect(
      await screen.findByRole("heading", { name: /Station Overview/i }),
    ).toBeInTheDocument();
  });
});
