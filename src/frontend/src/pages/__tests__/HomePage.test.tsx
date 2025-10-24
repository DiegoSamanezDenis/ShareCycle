import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import HomePage from "../HomePage";

vi.mock("../../api/client", () => {
  return {
    apiRequest: vi.fn(async () => [
      {
        stationId: "s1",
        name: "Downtown",
        status: "OCCUPIED",
        bikesAvailable: 5,
        bikesDocked: 7,
        capacity: 12,
        freeDocks: 5,
        latitude: 45.51,
        longitude: -73.57,
        fullnessCategory: "HEALTHY",
      },
    ]),
  };
});

describe("HomePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("renders map legend and loads stations for guests", async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    await waitFor(() =>
      expect(
        screen.getByRole("heading", { name: /Explore Stations/i }),
      ).toBeInTheDocument(),
    );

    expect(screen.getByText(/Legend:/i)).toBeInTheDocument();
    expect(screen.getByText(/Downtown/)).toBeInTheDocument();
  });
});
