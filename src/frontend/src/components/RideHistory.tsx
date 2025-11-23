import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchTripDetails, fetchTripHistory } from "../api/trips";
import type { TripHistoryFilters } from "../api/trips";
import type { BikeType, TripDetails, TripHistoryEntry, TripHistoryPage } from "../types/trip";

type RideHistoryProps = {
  token: string | null;
  isOperator: boolean;
};

type BikeFilter = "ALL" | BikeType;

type FiltersState = {
  startTime: string;
  endTime: string;
  bikeType: BikeFilter;
};

const initialFilters: FiltersState = {
  startTime: "",
  endTime: "",
  bikeType: "ALL",
};

const PAGE_SIZE = 8;

function normalizeDateTime(value: string): string | undefined {
  if (!value) {
    return undefined;
  }
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  const probe = new Date(trimmed);
  if (Number.isNaN(probe.getTime())) {
    return undefined;
  }
  const [datePart, timePartRaw] = trimmed.split("T");
  if (!datePart || !timePartRaw) {
    return undefined;
  }
  let timePart = timePartRaw;
  // Strip fractional seconds if present (e.g., ":30.123")
  if (timePart.includes(".")) {
    timePart = timePart.slice(0, timePart.indexOf("."));
  }
  // Ensure seconds are present (LocalDateTime parse prefers HH:mm:ss)
  const segments = timePart.split(":");
  if (segments.length === 2) {
    timePart = `${segments[0]}:${segments[1]}:00`;
  } else if (segments.length >= 3) {
    timePart = `${segments[0]}:${segments[1]}:${segments[2]}`;
  }
  return `${datePart}T${timePart}`;
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return date.toLocaleString();
}

function formatDuration(minutes: number): string {
  if (!Number.isFinite(minutes) || minutes < 0) {
    return "—";
  }
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  if (hours === 0) {
    return `${minutes} min`;
  }
  return `${hours}h ${remainder}m`;
}

function formatCurrency(amount: number): string {
  if (!Number.isFinite(amount)) {
    return "$0.00";
  }
  return `$${amount.toFixed(2)}`;
}

function renderBikeTypeLabel(type: BikeType | null): string {
  if (!type) {
    return "Unknown";
  }
  return type === "E_BIKE" ? "E-Bike" : "Standard";
}

function renderLedgerStatus(status: TripHistoryEntry["ledgerStatus"]): string {
  if (!status) {
    return "Pending";
  }
  return status === "PAID" ? "Paid" : "Pending";
}

export default function RideHistory({ token, isOperator }: RideHistoryProps) {
  const [filters, setFilters] = useState<FiltersState>(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState<{
    startTime?: string;
    endTime?: string;
    bikeType?: BikeType | null;
  }>({});
  const [historyPage, setHistoryPage] = useState<TripHistoryPage | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [tripSearch, setTripSearch] = useState("");
  const [debouncedTripSearch, setDebouncedTripSearch] = useState("");
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null);
  const [tripDetails, setTripDetails] = useState<TripDetails | null>(null);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);

  const appliedFilterSummary = useMemo(() => {
    return {
      startTime: appliedFilters.startTime ?? null,
      endTime: appliedFilters.endTime ?? null,
      bikeType: appliedFilters.bikeType ?? null,
    };
  }, [appliedFilters]);

  const loadHistory = useCallback(
    async (filtersToApply: typeof appliedFilters, pageToLoad: number, tripIdFilter: string) => {
      if (!token) {
        setHistoryPage(null);
        setHistoryError("You must be signed in to view ride history.");
        setLoadingHistory(false);
        return;
      }
      setLoadingHistory(true);
      setHistoryError(null);
      try {
        const requestFilters: TripHistoryFilters = {
          startTime: filtersToApply.startTime,
          endTime: filtersToApply.endTime,
          bikeType: filtersToApply.bikeType,
          tripId: tripIdFilter,
        };
        const page = await fetchTripHistory(requestFilters, token, {
          page: pageToLoad,
          pageSize: PAGE_SIZE,
        });
        if (page.totalPages > 0 && page.page >= page.totalPages) {
          setCurrentPage(page.totalPages - 1);
          return;
        }
        if (page.totalPages === 0 && page.page !== 0) {
          setCurrentPage(0);
          return;
        }
        setHistoryPage(page);
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "Unable to load trip history. Please try again.";
        setHistoryError(message);
        setHistoryPage(null);
      } finally {
        setLoadingHistory(false);
      }
    },
    [token],
  );

  const loadDetails = useCallback(
    async (tripId: string) => {
      if (!token) {
        setDetailsError("You must be signed in to view ride details.");
        return;
      }
      setLoadingDetails(true);
      setDetailsError(null);
      try {
        const details = await fetchTripDetails(tripId, token);
        setTripDetails(details);
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "Unable to load trip details. Please try again.";
        setTripDetails(null);
        setDetailsError(message);
      } finally {
        setLoadingDetails(false);
      }
    },
    [token],
  );

  useEffect(() => {
    const handle = window.setTimeout(() => {
      const normalized = tripSearch.trim();
      setDebouncedTripSearch((prev) => (prev === normalized ? prev : normalized));
    }, 300);
    return () => window.clearTimeout(handle);
  }, [tripSearch]);

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedTripSearch]);

  useEffect(() => {
    if (!token) {
      setHistoryPage(null);
      setHistoryError("You must be signed in to view ride history.");
      setLoadingHistory(false);
      return;
    }
    void loadHistory(appliedFilters, currentPage, debouncedTripSearch);
  }, [token, appliedFilters, currentPage, debouncedTripSearch, loadHistory]);

  const handleApplyFilters = () => {
    const nextFilters = {
      startTime: normalizeDateTime(filters.startTime),
      endTime: normalizeDateTime(filters.endTime),
      bikeType: filters.bikeType === "ALL" ? null : filters.bikeType,
    };
    setAppliedFilters(nextFilters);
    setCurrentPage(0);
  };

  const handleResetFilters = () => {
    setFilters(initialFilters);
    setAppliedFilters({});
    setCurrentPage(0);
  };

  const handleSelectTrip = async (tripId: string) => {
    setSelectedTripId(tripId);
    await loadDetails(tripId);
  };

  const historyEntries = historyPage?.entries ?? [];
  const totalItems = historyPage?.totalItems ?? 0;
  const totalPages = historyPage?.totalPages ?? 0;
  const selectedHistoryEntry = useMemo(() => {
    if (!selectedTripId) return null;
    return historyEntries.find((entry) => entry.tripId === selectedTripId) ?? null;
  }, [historyEntries, selectedTripId]);

  return (
    <div>
      <form
        onSubmit={(event) => {
          event.preventDefault();
          void handleApplyFilters();
        }}
        style={{
          display: "flex",
          gap: 16,
          flexWrap: "wrap",
          alignItems: "flex-end",
          marginBottom: 16,
        }}
      >
        <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          Start
          <input
            type="datetime-local"
            value={filters.startTime}
            onChange={(event) =>
              setFilters((prev) => ({ ...prev, startTime: event.target.value }))
            }
          />
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          End
          <input
            type="datetime-local"
            value={filters.endTime}
            onChange={(event) => setFilters((prev) => ({ ...prev, endTime: event.target.value }))}
          />
        </label>
        <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          Bike type
          <select
            value={filters.bikeType}
            onChange={(event) =>
              setFilters((prev) => ({ ...prev, bikeType: event.target.value as BikeFilter }))
            }
          >
            <option value="ALL">All bikes</option>
            <option value="STANDARD">Standard</option>
            <option value="E_BIKE">E-Bike</option>
          </select>
        </label>
        <button type="submit" disabled={loadingHistory}>
          Apply filters
        </button>
        <button
          type="button"
          onClick={() => void handleResetFilters()}
          disabled={loadingHistory && totalItems === 0}
        >
          Reset
        </button>
      </form>

      <div style={{ marginBottom: 12, fontSize: 13, color: "#4b5563" }}>
        <span>Applied filters:</span>{" "}
        <span>
          Start: {appliedFilterSummary.startTime ? formatDateTime(appliedFilterSummary.startTime) : "Any"}
        </span>
        {" | "}
        <span>
          End: {appliedFilterSummary.endTime ? formatDateTime(appliedFilterSummary.endTime) : "Any"}
        </span>
        {" | "}
        <span>
          Bike:{" "}
          {appliedFilterSummary.bikeType ? renderBikeTypeLabel(appliedFilterSummary.bikeType) : "All"}
        </span>
      </div>

      <div style={{ marginBottom: 12 }}>
        <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
          Search by Trip ID
          <input
            type="text"
            placeholder="e.g. 4f8c2a..."
            value={tripSearch}
            onChange={(event) => setTripSearch(event.target.value)}
          />
        </label>
      </div>

      {loadingHistory && <p>Loading ride history...</p>}
      {historyError && <p role="alert">{historyError}</p>}

      {!loadingHistory && !historyError && historyPage && (
        <>
          {totalItems === 0 ? (
            <p>
              {debouncedTripSearch
                ? "No trips match the current trip ID search."
                : "No rides matched the selected filters."}
            </p>
          ) : (
            <>
              <div style={{ overflowX: "auto" }}>
                <table style={{ minWidth: 760 }}>
                  <thead>
                    <tr>
                      <th>Trip</th>
                      {isOperator && <th>Rider</th>}
                      <th>Start</th>
                      <th>End</th>
                      <th>Started</th>
                      <th>Ended</th>
                      <th>Duration</th>
                      <th>Bike</th>
                      <th>Bike ID</th>
                      <th>Total</th>
                      <th>Ledger</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {historyEntries.map((entry) => {
                      const isSelected = entry.tripId === selectedTripId;
                      const riderLabel =
                        entry.riderName ??
                        (entry.riderId ? entry.riderId.slice(0, 8).toUpperCase() : "Unknown");
                      return (
                        <tr
                          key={entry.tripId}
                          style={
                            isSelected
                              ? {
                                  background: "#1f2937",
                                  color: "#f9fafb",
                                }
                              : undefined
                          }
                        >
                          <td>
                            <code>{entry.tripId.slice(0, 8).toUpperCase()}</code>
                          </td>
                          {isOperator && <td>{riderLabel}</td>}
                          <td>{entry.startStationName ?? "Unknown"}</td>
                          <td>{entry.endStationName ?? "—"}</td>
                          <td>{formatDateTime(entry.startTime)}</td>
                          <td>{formatDateTime(entry.endTime)}</td>
                          <td>{formatDuration(entry.durationMinutes)}</td>
                          <td>{renderBikeTypeLabel(entry.bikeType)}</td>
                          <td>
                            {entry.bikeId ? (
                              <code>{entry.bikeId.slice(0, 8).toUpperCase()}</code>
                            ) : (
                              "—"
                            )}
                          </td>
                          <td>{formatCurrency(entry.totalCost)}</td>
                          <td>{renderLedgerStatus(entry.ledgerStatus)}</td>
                          <td>
                            <button
                              type="button"
                              onClick={() => void handleSelectTrip(entry.tripId)}
                              disabled={loadingDetails && selectedTripId === entry.tripId}
                              style={
                                isSelected
                                  ? {
                                      background: "#f9fafb",
                                      color: "#111827",
                                      border: "1px solid #cbd5f5",
                                    }
                                  : undefined
                              }
                            >
                              {selectedTripId === entry.tripId ? "Refresh" : "View"}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {totalItems > PAGE_SIZE && (
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    marginTop: 12,
                    flexWrap: "wrap",
                    gap: 12,
                  }}
                >
                  <span>
                    Showing{" "}
                    {`${historyPage.page * historyPage.pageSize + 1}-${Math.min(
                      historyPage.page * historyPage.pageSize + historyEntries.length,
                      totalItems,
                    )}`}{" "}
                    of {totalItems} trip{totalItems === 1 ? "" : "s"}
                  </span>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button
                      type="button"
                      onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                      disabled={loadingHistory || !historyPage.hasPrevious}
                    >
                      Prev
                    </button>
                    <span>
                      Page {historyPage.page + 1} of {totalPages || 1}
                    </span>
                    <button
                      type="button"
                      onClick={() => setCurrentPage((prev) => prev + 1)}
                      disabled={loadingHistory || !historyPage.hasNext}
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </>
          )}

          {selectedTripId && (
            <div
              style={{
                marginTop: 24,
                padding: 20,
                border: "1px solid #1f2937",
                borderRadius: 12,
                background: "#111827",
                color: "#f9fafb",
                boxShadow: "0 12px 24px rgba(15, 23, 42, 0.35)",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h3 style={{ margin: 0, color: "#f9fafb" }}>
                  Trip Details — <code>{selectedTripId.slice(0, 8).toUpperCase()}</code>
                </h3>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedTripId(null);
                    setTripDetails(null);
                    setDetailsError(null);
                  }}
                >
                  Close
                </button>
              </div>

              {loadingDetails && <p>Loading trip details...</p>}
              {detailsError && <p role="alert">{detailsError}</p>}
              {!loadingDetails && !detailsError && tripDetails && (
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                    gap: 16,
                    marginTop: 16,
                  }}
                >
                  <div>
                    <strong>Rider</strong>
                    <p>{tripDetails.riderName ?? tripDetails.riderId ?? "Unknown"}</p>
                  </div>
                  <div>
                    <strong>Start station</strong>
                    <p>{tripDetails.startStationName ?? "Unknown"}</p>
                  </div>
                  <div>
                    <strong>End station</strong>
                    <p>{tripDetails.endStationName ?? "In progress"}</p>
                  </div>
                  <div>
                    <strong>Started</strong>
                    <p>{formatDateTime(tripDetails.startTime)}</p>
                  </div>
                  <div>
                    <strong>Ended</strong>
                    <p>{formatDateTime(tripDetails.endTime)}</p>
                  </div>
                  <div>
                    <strong>Duration</strong>
                    <p>{formatDuration(tripDetails.durationMinutes)}</p>
                  </div>
                  <div>
                    <strong>Bike type</strong>
                    <p>{renderBikeTypeLabel(tripDetails.bikeType)}</p>
                  </div>
                  <div>
                    <strong>Bike ID</strong>
                    <p>
                      {tripDetails.bikeId ? (
                        <code>{tripDetails.bikeId.slice(0, 8).toUpperCase()}</code>
                      ) : (
                        "Unknown"
                      )}
                    </p>
                  </div>
                  <div>
                    <strong>Ledger</strong>
                    <p>
                      {tripDetails.ledgerId ? (
                        <>
                          <code>{tripDetails.ledgerId.slice(0, 8).toUpperCase()}</code>{" "}
                          ({renderLedgerStatus(tripDetails.ledgerStatus)})
                        </>
                      ) : (
                        "Not yet billed"
                      )}
                    </p>
                  </div>
                  <div>
                    <strong>Cost breakdown</strong>
                    <ul style={{ paddingLeft: 20, margin: "4px 0" }}>
                      <li>Base: {formatCurrency(tripDetails.baseCost)}</li>
                      <li>Time: {formatCurrency(tripDetails.timeCost)}</li>
                      <li>E-Bike: {formatCurrency(tripDetails.eBikeSurcharge)}</li>
                      <li style={{ fontWeight: 700 }}>Total: {formatCurrency(tripDetails.totalCost)}</li>
                    </ul>
                  </div>
                </div>
              )}
              {!loadingDetails && !detailsError && !tripDetails && (
                <p>No details available for the selected trip.</p>
              )}
              {selectedHistoryEntry && !tripDetails && !loadingDetails && !detailsError && (
                <p>
                  This trip may still be in progress. Check back later for the full billing breakdown.
                </p>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}
