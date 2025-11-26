import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import TripSummaryPage from "../../pages/TripSummaryPage";
import { AuthProvider } from "../../auth/AuthContext";
import { apiRequest } from "../../api/client";

type AnyResponse = unknown;

async function defaultApiImplementation(path: string, opts?: RequestInit): Promise<AnyResponse> {
  if (path === "/trips/last-completed") {
    const now = new Date();
    return {
      tripId: "trip-1",
      endStationId: "station-3",
      endedAt: now.toISOString(),
      durationMinutes: 12,
      ledgerId: "ledger-abc",
      baseCost: 1.5,
      timeCost: 2.25,
      eBikeSurcharge: 0.75,
      totalCost: 4.5,
      ledgerStatus: "PENDING",
      paymentStatus: "PENDING",
      discountRate: 0.1,
      discountAmount: 0.45,
      flexCreditApplied: 0,
    };
  }
  if (path.startsWith("/auth/credit")) {
    return { amount: 2.5 };
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
  if (path.startsWith("/trips") && (!opts || !opts.method || opts.method === "GET")) {
    const start = new Date();
    const end = new Date(start.getTime() + 15 * 60000);
    return {
      entries: [
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
      ],
      page: 0,
      pageSize: 8,
      totalItems: 1,
      totalPages: 1,
      hasNext: false,
      hasPrevious: false,
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

function renderWithAuth(override?: AuthOverride) {
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
      <MemoryRouter>
        <TripSummaryPage />
      </MemoryRouter>
    </AuthProvider>,
  );
}

describe("TripSummaryPage ride history", () => {
  beforeEach(() => {
    localStorage.clear();
    mockedApi.mockImplementation(defaultApiImplementation);
  });

  it("renders ride history with returned trips", async () => {
    renderWithAuth();

    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historySection = historyHeading.closest("section") as HTMLElement;
    const historyTable = within(historySection).getByRole("table");
    const historyRegion = within(historyTable as HTMLTableElement);
    expect(historyRegion.getByText(/HISTORY-/i)).toBeInTheDocument();
    expect(historyRegion.getByText("Station #1")).toBeInTheDocument();
    expect(historyRegion.getByText("Station #2")).toBeInTheDocument();
    expect(historyRegion.getByText("$3.75")).toBeInTheDocument();
  });

  it("paginates ride history when more than one page of trips exists", async () => {
    mockedApi.mockImplementation(async (path: string, opts?: RequestInit) => {
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

    renderWithAuth();

    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historySection = historyHeading.closest("section") as HTMLElement;
    expect(
      within(historySection).getByText((content) => content.includes("Page 1 of 2")),
    ).toBeInTheDocument();
    const nextButton = within(historySection).getByRole("button", { name: /^Next$/i });
    expect(nextButton).toBeEnabled();
    fireEvent.click(nextButton);
    await waitFor(() => {
      const matches = within(historySection).getAllByText(/Station 10/i);
      expect(matches.length).toBeGreaterThan(0);
    });
    const prevButton = within(historySection).getByRole("button", { name: /^Prev$/i });
    expect(prevButton).toBeEnabled();
  });

  it("renders ride history for operators as well", async () => {
    renderWithAuth({
      role: "OPERATOR",
      userId: "op1",
      username: "operator",
    });

    const historyHeading = await screen.findByRole("heading", { name: /Ride History/i });
    const historySection = historyHeading.closest("section") as HTMLElement;
    const historyTable = within(historySection).getByRole("table");
    const headerCells = within(historyTable as HTMLTableElement).getAllByRole("columnheader");
    expect(headerCells.some((cell) => cell.textContent === "Rider")).toBe(true);
  });

  it("shows loyalty discount row when discount data is returned", async () => {
    renderWithAuth();

    const discountRow = await screen.findByText(/Loyalty discount \(10%/i);
    expect(discountRow).toHaveTextContent("-$0.45");
    expect(screen.queryByText(/^0$/)).toBeNull();
  });
});
