import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, within } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { AuthProvider } from "../../auth/AuthContext";
import { routes } from "../../routes";

vi.mock("../../api/client", () => {
  return {
    apiRequest: vi.fn(async (path: string, opts?: RequestInit) => {
      if (path === "/stations" && (!opts || !opts.method)) {
        return [
          {
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
          },
        ];
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
      if (path === "/public/events") {
        return [];
      }
      if (path === "/pricing") {
        return [
          {
            planId: "p1",
            name: "Pay As You Go",
            description: "Pay for each ride.",
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
      if (path === "/reservations" && opts?.method === "POST") {
        return {
          reservationId: "r1",
          stationId: "s1",
          bikeId: "b1",
          reservedAt: new Date().toISOString(),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
          active: true,
        };
      }
      if (path === "/trips" && opts?.method === "POST") {
        return {
          tripId: "t1",
          stationId: "s1",
          bikeId: "b1",
          riderId: "u1",
          startedAt: new Date().toISOString(),
        };
      }
      if (path.startsWith("/trips/") && opts?.method === "POST") {
        return {
          status: "COMPLETED",
          tripId: "t1",
          stationId: "s1",
          endedAt: new Date().toISOString(),
          durationMinutes: 3,
          ledgerId: "l1",
          baseCost: 0.5,
          timeCost: 1.0,
          eBikeSurcharge: 0.25,
          totalCost: 1.75,
          message: "Trip completed successfully.",
          suggestions: [],
        };
      }
      if (path.includes("/status") && opts?.method === "PATCH") {
        return {
          stationId: "s1",
          name: "Station #1",
          status: "OUT_OF_SERVICE",
          bikesAvailable: 0,
          bikesDocked: 3,
          capacity: 5,
          freeDocks: 2,
          latitude: 45.5,
          longitude: -73.6,
          fullnessCategory: "FULL",
        };
      }
      if (path.includes("/capacity") && opts?.method === "PATCH") {
        return {
          stationId: "s1",
          name: "Station #1",
          status: "OCCUPIED",
          bikesAvailable: 2,
          bikesDocked: 3,
          capacity: 6,
          freeDocks: 3,
          latitude: 45.5,
          longitude: -73.6,
          fullnessCategory: "HEALTHY",
        };
      }
      if (path === "/stations/move-bike" && opts?.method === "POST") {
        return [
          {
            stationId: "s1",
            name: "Station #1",
            status: "OCCUPIED",
            bikesAvailable: 1,
            bikesDocked: 2,
            capacity: 5,
            freeDocks: 3,
            latitude: 45.5,
            longitude: -73.6,
            fullnessCategory: "LOW",
          },
        ];
      }
      return undefined;
    }),
  };
});

function renderDashboardWithAuth(
  initialEntries: string[],
  auth: {
    token: string;
    role: "RIDER" | "OPERATOR";
    userId: string;
    username: string;
  },
) {
  localStorage.setItem("sharecycle.auth", JSON.stringify(auth));
  const router = createMemoryRouter(routes, { initialEntries });
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
}

describe("Rider flows", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("starts and ends a trip with feedback/receipt", async () => {
    renderDashboardWithAuth(["/dashboard"], {
      token: "t",
      role: "RIDER",
      userId: "u1",
      username: "r1",
    });
    // Wait for station overview
    await screen.findByRole("heading", { name: /Station Overview/i });

    fireEvent.click(screen.getByRole("button", { name: /View/i }));

    const reserveButton = await screen.findByRole("button", {
      name: /^Reserve$/i,
    });
    fireEvent.click(reserveButton);
    expect(
      await screen.findByText(/Reservation created successfully/i),
    ).toBeInTheDocument();

    const startButton = await screen.findByRole("button", {
      name: /Start trip/i,
    });
    fireEvent.click(startButton);
    expect(await screen.findByText(/Trip started\./i)).toBeInTheDocument();

    const endButton = await screen.findByRole("button", {
      name: /End trip here/i,
    });
    fireEvent.click(endButton);
    expect(await screen.findByText(/Trip completed\./i)).toBeInTheDocument();
    expect(screen.getByText(/total \$1\.75/i)).toBeInTheDocument();
  });
});

describe("Operator controls", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("updates status, capacity, and moves a bike", async () => {
    renderDashboardWithAuth(["/dashboard"], {
      token: "t",
      role: "OPERATOR",
      userId: "op1",
      username: "ops",
    });
    await screen.findByRole("heading", { name: /Operator Controls/i });

    // Toggle station status
    const statusHeading = screen.getByRole("heading", {
      name: /Toggle station status/i,
    });
    const statusForm = statusHeading.closest("form") as HTMLFormElement;
    const st = within(statusForm);
    fireEvent.change(st.getByLabelText(/Station ID/i), {
      target: { value: "s1" },
    });
    fireEvent.click(st.getByRole("checkbox", { name: /Out of service/i }));
    fireEvent.click(st.getByRole("button", { name: /Update status/i }));
    expect(
      await screen.findByText(/Station status updated\./i),
    ).toBeInTheDocument();

    // Adjust capacity
    const capHeading = screen.getByRole("heading", {
      name: /Adjust capacity/i,
    });
    const capForm = capHeading.closest("form") as HTMLFormElement;
    const c = within(capForm);
    fireEvent.change(c.getByLabelText(/^Station ID$/i), {
      target: { value: "s1" },
    });
    fireEvent.change(c.getByLabelText(/Delta/i), { target: { value: "1" } });
    fireEvent.click(c.getByRole("button", { name: /Apply change/i }));
    expect(
      await screen.findByText(/Station capacity updated\./i),
    ).toBeInTheDocument();

    // Move bike
    const moveHeading = screen.getByRole("heading", { name: /Move a bike/i });
    const moveForm = moveHeading.closest("form") as HTMLFormElement;
    const m = within(moveForm);
    fireEvent.change(m.getByLabelText(/Bike ID/i), { target: { value: "b2" } });
    fireEvent.change(m.getByLabelText(/Destination Station ID/i), {
      target: { value: "s1" },
    });
    fireEvent.click(m.getByRole("button", { name: /Move bike/i }));
    expect(
      await screen.findByText(/Bike moved successfully\./i),
    ).toBeInTheDocument();
  });
});
