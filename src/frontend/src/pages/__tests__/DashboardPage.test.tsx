import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
} from "@testing-library/react";
import DashboardPage from "../../pages/DashboardPage";
import { AuthProvider } from "../../auth/AuthContext";

vi.mock("../../api/client", () => {
  return {
    apiRequest: vi.fn(async (path: string, opts?: any) => {
      if (path === "/stations" && (!opts || opts.method === undefined)) {
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
      if (path === "/reservations" && opts?.method === "POST") {
        return {
          reservationId: "r1",
          stationId: "s1",
          bikeId: "b1",
          reservedAt: new Date().toISOString(),
          expiresAt: new Date(Date.now() + 2 * 60 * 1000).toISOString(),
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
          tripId: "t1",
          endStationId: "s2",
          endedAt: new Date().toISOString(),
          durationMinutes: 5,
          ledgerId: "l1",
          totalAmount: 2.5,
        };
      }
      return undefined;
    }),
  };
});

function renderWithAuth(ui: React.ReactElement) {
  // Seed localStorage for AuthContext
  localStorage.setItem(
    "sharecycle.auth",
    JSON.stringify({
      token: "demo",
      role: "RIDER",
      userId: "u1",
      username: "rider1",
    }),
  );
  return render(<AuthProvider>{ui}</AuthProvider>);
}

describe("DashboardPage", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("renders station overview and map", async () => {
    renderWithAuth(<DashboardPage />);
    // Wait for overview to load
    const overviewHeading = await screen.findByRole("heading", {
      name: /Station Overview/i,
    });
    // Find table under overview and assert station name inside table (avoid map overlay duplicate)
    const table = overviewHeading.parentElement?.querySelector(
      "table",
    ) as HTMLTableElement;
    expect(table).toBeTruthy();
    expect(within(table).getByText("Station #1")).toBeInTheDocument();
    // Map section present
    expect(
      screen.getByRole("heading", { name: /City Map/i }),
    ).toBeInTheDocument();
  });

  it("creates a reservation from station details and shows feedback", async () => {
    renderWithAuth(<DashboardPage />);
    const viewButton = await screen.findByRole("button", { name: /View/i });
    fireEvent.click(viewButton);

    const stationDetails = await screen.findByText(/Dock d1/i);
    expect(stationDetails).toBeInTheDocument();

    const reserveButton = await screen.findByRole("button", {
      name: /^Reserve$/i,
    });
    fireEvent.click(reserveButton);

    await waitFor(() =>
      expect(
        screen.getByText(/Reservation created successfully/i),
      ).toBeInTheDocument(),
    );

    expect(
      screen.getByText(/Reservations hold a bike for 5 minutes/i),
    ).toBeInTheDocument();
  });
});
