import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent, waitFor, within } from "@testing-library/react";
import DashboardPage from "../../pages/DashboardPage";
import { AuthProvider } from "../../auth/AuthContext";
import { apiRequest } from "../../api/client";
import { MemoryRouter } from "react-router-dom";

type AnyResponse = unknown;

async function defaultApiImplementation(path: string, opts?: RequestInit): Promise<AnyResponse> {
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
  if (path === "/system/reset" && opts?.method === "POST") {
    return {
      bikes: 21,
      stations: 10,
      docks: 50,
    };
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
        description: "Pay per ride.",
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
      status: "COMPLETED",
      tripId: "t1",
      stationId: "s2",
      endedAt: new Date().toISOString(),
      durationMinutes: 5,
      ledgerId: "l1",
      baseCost: 0.5,
      timeCost: 1.5,
      eBikeSurcharge: 0.5,
      totalCost: 2.5,
      message: "Trip completed successfully.",
      suggestions: [],
    };
  }
  if (path.startsWith("/trips") && (!opts || !opts.method || opts.method === "GET")) {
    const url = new URL(path, "http://localhost");
    const page = Number.parseInt(url.searchParams.get("page") ?? "0", 10);
    const pageSize = Number.parseInt(url.searchParams.get("pageSize") ?? "8", 10);
    const start = new Date();
    const end = new Date(start.getTime() + 15 * 60000);
    const allEntries = [
      {
        tripId: "history-1",
        riderId: "u1",
        riderName: "Rider One",
        startStationName: "Station #1",
        endStationName: "Station #2",
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        durationMinutes: 15,
        bikeType: "E_BIKE",
        totalCost: 3.75,
        ledgerId: "ledger-1",
        ledgerStatus: "PAID",
      },
    ];
    const pagedEntries = allEntries.slice(page * pageSize, page * pageSize + pageSize);
    const totalItems = allEntries.length;
    const totalPages = totalItems === 0 ? 0 : 1;
    return {
      entries: pagedEntries,
      page,
      pageSize,
      totalItems,
      totalPages,
      hasNext: false,
      hasPrevious: page > 0,
    };
  }
  if (path.startsWith("/trips/") && (!opts || !opts.method || opts.method === "GET")) {
    const endTime = new Date();
    const startTime = new Date(endTime.getTime() - 20 * 60000);
    return {
      tripId: "history-1",
      riderId: "u1",
      riderName: "Rider One",
      startStationName: "Station #1",
      endStationName: "Station #2",
      startTime: startTime.toISOString(),
      endTime: endTime.toISOString(),
      durationMinutes: 20,
      bikeType: "E_BIKE",
      baseCost: 1.0,
      timeCost: 2.0,
      eBikeSurcharge: 0.75,
      totalCost: 3.75,
      ledgerId: "ledger-1",
      ledgerStatus: "PAID",
    };
  }
  return undefined;
}

vi.mock("../../api/client", () => ({
  apiRequest: vi.fn(defaultApiImplementation),
}));

const mockedApi = vi.mocked(apiRequest);

type AuthOverride = {
  token?: string;
  role?: string;
  userId?: string;
  username?: string;
};

function renderWithAuth(ui: React.ReactElement, override?: AuthOverride) {
  const authPayload = {
    token: "demo",
    role: "RIDER",
    userId: "u1",
    username: "rider1",
    ...override,
  };
  localStorage.setItem("sharecycle.auth", JSON.stringify(authPayload));
  return render(
    <AuthProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </AuthProvider>,
  );
}

async function openFirstStationRowAndClickView() {
  const overviewHeading = await screen.findByRole("heading", {
    name: /Station Overview/i,
  });
  const stationTable = overviewHeading.parentElement?.querySelector("table") as HTMLTableElement | null;
  expect(stationTable).not.toBeNull();
  const viewButtons = within(stationTable as HTMLTableElement).getAllByRole("button", { name: /^View$/i });
  fireEvent.click(viewButtons[0]);
}

describe("DashboardPage", () => {
  beforeEach(() => {
    localStorage.clear();
    mockedApi.mockImplementation(defaultApiImplementation);
  });

  it("renders station overview and map", async () => {
    renderWithAuth(<DashboardPage />);

    const overviewHeading = await screen.findByRole("heading", {
      name: /Station Overview/i,
    });
    const table = overviewHeading.parentElement?.querySelector("table") as HTMLTableElement;
    expect(table).toBeTruthy();
    expect(within(table).getByText("Station #1")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: /City Map/i })).toBeInTheDocument();
  });

  it("creates a reservation from station details and shows feedback", async () => {
    renderWithAuth(<DashboardPage />);
    await openFirstStationRowAndClickView();
    const stationDetails = await screen.findByText(/Dock d1/i);
    expect(stationDetails).toBeInTheDocument();

    const reserveButton = await screen.findByRole("button", { name: /^Reserve$/i });
    fireEvent.click(reserveButton);

    await waitFor(() =>
      expect(screen.getByText(/Reservation created successfully/i)).toBeInTheDocument(),
    );

    expect(screen.getByText(/Reservations hold a bike for 5 minutes/i)).toBeInTheDocument();
  });

  it("shows blocked return guidance when station is full", async () => {
    const blockedResponse = {
      status: "BLOCKED" as const,
      tripId: "t1",
      stationId: "s1",
      message: "Station is full. Try one of the nearby stations.",
      credit: null,
      suggestions: [
        {
          stationId: "alt1",
          name: "Station #2",
          freeDocks: 3,
          distanceMeters: 120,
        },
      ],
    };

    mockedApi.mockImplementation(async (path: string, opts?: RequestInit) => {
      if (path.startsWith("/trips/") && opts?.method === "POST") {
        return blockedResponse;
      }
      return defaultApiImplementation(path, opts);
    });

    renderWithAuth(<DashboardPage />);

    await openFirstStationRowAndClickView();

    const startButton = await screen.findByRole("button", { name: /Start trip/i });
    fireEvent.click(startButton);
    await screen.findByText(/Trip started\./i);

    const endButton = await screen.findByRole("button", { name: /End trip here/i });
    fireEvent.click(endButton);

    await waitFor(() => {
      const messages = screen.getAllByText(/Station is full\. Try one of the nearby stations\./i);
      expect(messages.length).toBeGreaterThan(0);
    });
    expect(screen.getByText(/Nearby stations with free docks/i)).toBeInTheDocument();
    const alternativeStations = screen.getAllByText(/Station #2/i);
    expect(alternativeStations.length).toBeGreaterThan(0);
    expect(screen.queryByText(/Trip Completed/i)).not.toBeInTheDocument();
  });

  it("renders ride history with returned trips", async () => {
    renderWithAuth(<DashboardPage />);

    expect(await screen.findByRole("heading", { name: /Ride History/i })).toBeInTheDocument();
    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historyTable = historyHeading.parentElement?.querySelector("table") as HTMLTableElement | null;
    expect(historyTable).not.toBeNull();
    const historyRegion = within(historyTable as HTMLTableElement);
    expect(historyRegion.getByText(/HISTORY-/)).toBeInTheDocument();
    expect(historyRegion.getByText("Station #1")).toBeInTheDocument();
    expect(historyRegion.getByText("Station #2")).toBeInTheDocument();
    expect(historyRegion.getByText("$3.75")).toBeInTheDocument();
  });

  it("paginates ride history when more than one page of trips exists", async () => {
    mockedApi.mockImplementation((path: string, opts?: RequestInit) => {
      if (path.startsWith("/trips") && (!opts || !opts.method || opts.method === "GET")) {
        const url = new URL(path, "http://localhost");
        const page = Number.parseInt(url.searchParams.get("page") ?? "0", 10);
        const pageSize = Number.parseInt(url.searchParams.get("pageSize") ?? "8", 10);
        const totalItems = 10;
        const allEntries = Array.from({ length: totalItems }, (_, idx) => {
          const start = new Date();
          const end = new Date(start.getTime() + 10 * 60000);
          return {
            tripId: `pagetrip-${idx}`,
            riderId: `r${idx}`,
            riderName: `Rider ${idx}`,
            startStationName: `Station ${idx + 1}`,
            endStationName: `Station ${idx + 1}`,
            startTime: start.toISOString(),
            endTime: end.toISOString(),
            durationMinutes: 10,
            bikeType: "STANDARD",
            totalCost: 2.5,
            ledgerId: `ledger-${idx}`,
            ledgerStatus: "PAID",
          };
        });
        const totalPages = Math.ceil(totalItems / pageSize);
        const entries = allEntries.slice(page * pageSize, page * pageSize + pageSize);
        return {
          entries,
          page,
          pageSize,
          totalItems,
          totalPages,
          hasNext: page < totalPages - 1,
          hasPrevious: page > 0,
        };
      }
      return defaultApiImplementation(path, opts);
    });

    renderWithAuth(<DashboardPage />);

    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historySection = historyHeading.closest("section") as HTMLElement;
    expect(
      within(historySection).getByText((content) => content.includes("Page 1 of 2")),
    ).toBeInTheDocument();
    const nextButton = within(historySection).getByRole("button", { name: /^Next$/i });
    expect(nextButton).toBeEnabled();
    fireEvent.click(nextButton);
    await waitFor(() =>
      expect(within(historySection).getByText(/Station 10/i)).toBeInTheDocument(),
    );
    const prevButton = within(historySection).getByRole("button", { name: /^Prev$/i });
    expect(prevButton).toBeEnabled();
  });

  it("renders ride history for operators as well", async () => {
    renderWithAuth(<DashboardPage />, {
      role: "OPERATOR",
      userId: "op1",
      username: "operator",
    });

    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historyTable = historyHeading.parentElement?.querySelector("table") as HTMLTableElement;
    expect(historyTable).toBeTruthy();
    const headerCells = within(historyTable as HTMLTableElement).getAllByRole("columnheader");
    expect(headerCells.some((cell) => cell.textContent === "Rider")).toBe(true);
  });

  it("allows operators to reset the system", async () => {
    renderWithAuth(<DashboardPage />, {
      role: "OPERATOR",
      userId: "op1",
      username: "operator",
    });

    expect(await screen.findByRole("heading", { name: /Operator Controls/i })).toBeInTheDocument();
    const resetButton = screen.getByRole("button", { name: /Reset system/i });
    fireEvent.click(resetButton);

    await waitFor(() =>
      expect(mockedApi).toHaveBeenCalledWith(
        "/system/reset",
        expect.objectContaining({ method: "POST" }),
      ),
    );
    expect(
      await screen.findByText(/System reset to initial dataset \(10 stations, 21 bikes\)\./i),
    ).toBeInTheDocument();
  });
});

