import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, within, waitFor } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { AuthProvider } from "../../auth/AuthContext";
import { routes } from "../../routes";

const E_BIKE_ID = "11111111-1111-1111-1111-111111111111";

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
            eBikesDocked: 1,
            eBikesAvailable: 1,
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
          eBikesDocked: 1,
          eBikesAvailable: 1,
          capacity: 5,
          freeDocks: 2,
          latitude: 45.5,
          longitude: -73.6,
          fullnessCategory: "HEALTHY",
          docks: [
            { dockId: "d1", status: "OCCUPIED", bikeId: E_BIKE_ID, bikeType: "E_BIKE" },
            { dockId: "d2", status: "EMPTY", bikeId: null, bikeType: null },
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
          bikeId: E_BIKE_ID,
          reservedAt: new Date().toISOString(),
          expiresAt: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
          active: true,
        };
      }
      if (path === "/trips" && (!opts || !opts.method || opts.method === "GET")) {
        const start = new Date();
        const end = new Date(start.getTime() + 10 * 60000);
        return [
          {
            tripId: "history-1",
            riderId: "u1",
            riderName: "Rider One",
            startStationName: "Station #1",
            endStationName: "Station #2",
            startTime: start.toISOString(),
            endTime: end.toISOString(),
            durationMinutes: 10,
            bikeType: "STANDARD",
            totalCost: 2.25,
            ledgerId: "ledger-1",
            ledgerStatus: "PAID",
          },
        ];
      }
      if (path === "/trips" && opts?.method === "POST") {
        return {
          tripId: "t1",
          stationId: "s1",
          bikeId: E_BIKE_ID,
          riderId: "u1",
          startedAt: new Date().toISOString(),
        };
      }
      if (path.startsWith("/trips/") && (!opts || !opts.method || opts.method === "GET")) {
        const end = new Date();
        const start = new Date(end.getTime() - 10 * 60000);
        return {
          tripId: "history-1",
          riderId: "u1",
          riderName: "Rider One",
          startStationName: "Station #1",
          endStationName: "Station #2",
          startTime: start.toISOString(),
          endTime: end.toISOString(),
          durationMinutes: 10,
          bikeType: "STANDARD",
          baseCost: 0.75,
          timeCost: 1.0,
          eBikeSurcharge: 0,
          totalCost: 1.75,
          ledgerId: "ledger-1",
          ledgerStatus: "PAID",
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
          eBikesDocked: 1,
          eBikesAvailable: 0,
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
          eBikesDocked: 1,
          eBikesAvailable: 1,
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
            eBikesDocked: 1,
            eBikesAvailable: 1,
            capacity: 5,
            freeDocks: 3,
            latitude: 45.5,
            longitude: -73.6,
            fullnessCategory: "LOW",
          },
        ];
      }
      if (path.startsWith("/auth/credit")) {
        return { amount: 0 };
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

    const viewButtons = await screen.findAllByRole("button", { name: /View/i });
    fireEvent.click(viewButtons[0]);

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
    expect(await screen.findByText(/^Trip Completed$/i)).toBeInTheDocument();
    expect(screen.getByText(/Total Charge: \$1\.75/i)).toBeInTheDocument();
  });

  it("surfaces e-bike indicators on map markers and dock cards", async () => {
    renderDashboardWithAuth(["/dashboard"], {
      token: "t",
      role: "RIDER",
      userId: "u1",
      username: "r1",
    });

    await screen.findByRole("heading", { name: /Station Overview/i });
    await waitFor(() => {
      expect(
        screen.getByTitle(/contains e-bikes/i),
      ).toBeInTheDocument();
    });

    const viewButtons = await screen.findAllByRole("button", { name: /View/i });
    fireEvent.click(viewButtons[0]);

    await waitFor(() => {
      expect(
        screen.getByText(new RegExp(`Bike ${E_BIKE_ID.slice(0, 8)} • E-Bike`, "i")),
      ).toBeInTheDocument();
    });
    expect(screen.getByTitle(/E-bike docked/i)).toBeInTheDocument();
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

    const viewButtons = await screen.findAllByRole("button", { name: /View/i });
    fireEvent.click(viewButtons[0]);

    const toggleButton = await screen.findByRole("button", { name: /Toggle Status/i });
    fireEvent.click(toggleButton);
    expect(await screen.findByText(/Station status updated\./i)).toBeInTheDocument();

    const addDockButton = await screen.findByRole("button", { name: /\+1 Dock/i });
    fireEvent.click(addDockButton);
    expect(await screen.findByText(/Station capacity updated\./i)).toBeInTheDocument();

    const moveHeading = screen.getByRole("heading", { name: /Move a bike/i });
    const moveForm = moveHeading.closest("form") as HTMLFormElement;
    const m = within(moveForm);
    fireEvent.change(m.getByLabelText(/Bike ID/i), { target: { value: E_BIKE_ID } });
    fireEvent.change(m.getByLabelText(/Destination ID/i), { target: { value: "s1" } });
    fireEvent.click(m.getByRole("button", { name: /Move bike/i }));
    expect(await screen.findByText(/Bike moved successfully\./i)).toBeInTheDocument();
  });
});
