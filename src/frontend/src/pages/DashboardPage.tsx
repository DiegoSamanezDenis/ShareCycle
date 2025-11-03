import { useCallback, useEffect, useMemo, useState } from "react";

import { Map, Marker as PigeonMarker } from "pigeon-maps";

import type { FormEvent } from "react";

import { apiRequest } from "../api/client";

import { useAuth } from "../auth/AuthContext";

import type { StationSummary, StationDetails } from "../types/station";

import type { DomainEventEntry } from "../types/events";

type ReservationResponse = {
  reservationId: string;

  stationId: string;

  bikeId: string;

  reservedAt: string;

  expiresAt: string;

  active: boolean;
};

type TripResponse = {
  tripId: string;

  stationId: string;

  bikeId: string;

  riderId: string;

  startedAt: string;
};

type TripCompletionResponse = {
  tripId: string;

  endStationId: string;

  endedAt: string;

  durationMinutes: number;

  ledgerId: string;

  totalAmount: number;
};

// Removed CapacityFormState; no standalone capacity form

type MoveBikeFormState = {
  bikeId: string;

  destinationId: string;
};

const DEFAULT_RESERVATION_MINUTES = 5;

const DOCK_BACKGROUND: Record<string, { background: string; border: string }> =
  Object.freeze({
    RESERVED: { background: "#fff7ed", border: "#f97316" },
    OCCUPIED: { background: "#e0f2fe", border: "#0369a1" },
    EMPTY: { background: "#ecfdf5", border: "#047857" },
    OUT_OF_SERVICE: { background: "#fee2e2", border: "#b91c1c" },
  });

type RideAction = "reserve" | "start" | "end" | null;

const ACTIVE_TRIP_STORAGE_KEY = "sharecycle.activeTrip";

function getActiveTripKeyForUser(userId?: string | null): string {
  return userId ? `${ACTIVE_TRIP_STORAGE_KEY}.${userId}` : ACTIVE_TRIP_STORAGE_KEY;
}

function readActiveTripFromStorage(userId?: string | null): TripResponse | null {
  if (typeof window === "undefined") {
    return null;
  }
  try {
    // Prefer user-scoped key
    const scopedKey = getActiveTripKeyForUser(userId);
    let raw = window.localStorage.getItem(scopedKey);
    // Backward-compat: migrate from legacy shared key if present
    if (!raw) {
      const legacy = window.localStorage.getItem(ACTIVE_TRIP_STORAGE_KEY);
      if (legacy) {
        try {
          const legacyParsed = JSON.parse(legacy) as Partial<TripResponse>;
          if (legacyParsed && legacyParsed.riderId && legacyParsed.riderId === userId) {
            window.localStorage.setItem(scopedKey, legacy);
          }
        } finally {
          // Always remove legacy key to avoid leakage across users/roles
          window.localStorage.removeItem(ACTIVE_TRIP_STORAGE_KEY);
        }
        raw = window.localStorage.getItem(scopedKey);
      }
    }
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<TripResponse>;
    if (
      parsed &&
      typeof parsed.tripId === "string" &&
      typeof parsed.stationId === "string" &&
      typeof parsed.bikeId === "string"
    ) {
      const trip: TripResponse = {
        tripId: parsed.tripId,
        stationId: parsed.stationId,
        bikeId: parsed.bikeId,
        riderId: parsed.riderId ?? "",
        startedAt: parsed.startedAt ?? new Date().toISOString(),
      };
      // Ensure trip belongs to current user
      if (userId && trip.riderId && trip.riderId !== userId) {
        window.localStorage.removeItem(scopedKey);
        return null;
      }
      return trip;
    }
  } catch {
    // ignore corrupt storage
  }
  return null;
}

function writeActiveTripToStorage(userId: string | null, trip: TripResponse | null): void {
  if (typeof window === "undefined") {
    return;
  }
  if (trip) {
    const scopedKey = getActiveTripKeyForUser(userId);
    window.localStorage.setItem(
      scopedKey,
      JSON.stringify({
        tripId: trip.tripId,
        stationId: trip.stationId,
        bikeId: trip.bikeId,
        riderId: trip.riderId,
        startedAt: trip.startedAt,
      }),
    );
  } else {
    const scopedKey = getActiveTripKeyForUser(userId);
    window.localStorage.removeItem(scopedKey);
  }
}

// Removed defaultCapacityForm; inline capacity adjustment is used instead

const defaultMoveBikeForm: MoveBikeFormState = {
  bikeId: "",

  destinationId: "",
};

const STATION_STATUS_LABEL: Record<string, string> = Object.freeze({
  EMPTY: "Empty",
  OCCUPIED: "Occupied",
  FULL: "Full",
  OUT_OF_SERVICE: "Out of service",
});

const DOCK_STATUS_LABEL: Record<string, string> = Object.freeze({
  EMPTY: "Empty",
  OCCUPIED: "Occupied",
  RESERVED: "Reserved",
  OUT_OF_SERVICE: "Out of service",
});

function formatStationStatus(status?: string | null): string {
  if (!status) return "Unknown";
  return STATION_STATUS_LABEL[status] ?? status;
}

function formatDockStatus(status?: string | null): string {
  if (!status) return "Unknown";
  return DOCK_STATUS_LABEL[status] ?? status;
}

function isUuid(value: string): boolean {
  return /^[0-9a-fA-F-]{8}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{4}-[0-9a-fA-F-]{12}$/.test(
    value.trim(),
  );
}

function isShortHex(value: string): boolean {
  return /^[0-9a-fA-F]{8}$/.test(value.trim());
}

function resolveDestinationStationIdFromInput(
  input: string,
  selectedStationId: string | null,
  stationDetails: StationDetails | null,
  stations: StationSummary[],
): { stationId: string | null; error?: string } {
  const value = input.trim();
  const compact = value.replace(/-/g, "").toLowerCase();
  if (isUuid(value)) {
    return { stationId: value };
  }
  if (isShortHex(value)) {
    if (!selectedStationId || !stationDetails || stationDetails.stationId !== selectedStationId) {
      return { stationId: null, error: "Select a station to resolve a dock short ID." };
    }
    const matches = stationDetails.docks.filter((d) => {
      const dockCompact = d.dockId.replace(/-/g, "").toLowerCase();
      const bikeCompact = d.bikeId ? d.bikeId.replace(/-/g, "").toLowerCase() : "";
      return dockCompact.startsWith(compact) || (bikeCompact && bikeCompact.startsWith(compact));
    });
    if (matches.length === 1) {
      return { stationId: stationDetails.stationId };
    }
    if (matches.length > 1) {
      return { stationId: null, error: "Dock ID is ambiguous; enter full ID." };
    }
    return { stationId: null, error: "Dock ID not found in selected station." };
  }
  // Try exact stationId string (non-UUID user input) — must match one of summaries
  const station = stations.find((s) => s.stationId === value);
  if (station) {
    return { stationId: station.stationId };
  }
  // Try station short ID (prefix) across all stations
  const stationMatches = stations.filter((s) => s.stationId.replace(/-/g, "").toLowerCase().startsWith(compact));
  if (stationMatches.length === 1) {
    return { stationId: stationMatches[0].stationId };
  }
  return { stationId: null, error: "Enter a valid Station ID or Dock ID." };
}

async function resolveDestinationStationIdAsync(
  input: string,
  selectedStationId: string | null,
  stationDetails: StationDetails | null,
  stations: StationSummary[],
  token: string | null,
): Promise<{ stationId: string | null; error?: string }> {
  // First, try fast local resolution
  const quick = resolveDestinationStationIdFromInput(
    input,
    selectedStationId,
    stationDetails,
    stations,
  );
  if (quick.stationId || isUuid(input.trim())) {
    return quick;
  }
  // If short hex provided and no selected station, search all stations' docks
  const compact = input.trim().replace(/-/g, "").toLowerCase();
  if (!isShortHex(compact)) {
    return quick;
  }
  const matches: string[] = [];
  for (const s of stations) {
    try {
      const details = await apiRequest<StationDetails>(
        `/stations/${s.stationId}/details`,
        { token: token ?? undefined },
      );
      const has = details.docks.some((d) =>
        d.dockId.replace(/-/g, "").toLowerCase().startsWith(compact),
      );
      if (has) {
        matches.push(s.stationId);
        if (matches.length > 1) {
          // Ambiguous across stations
          return { stationId: null, error: "Dock ID matches multiple stations; enter full ID." };
        }
      }
    } catch {
      // ignore individual station details failures
    }
  }
  if (matches.length === 1) {
    return { stationId: matches[0] };
  }
  return { stationId: null, error: "Dock ID not found; select a station or enter full ID." };
}

async function resolveBikeIdAsync(
  input: string,
  selectedStationId: string | null,
  stationDetails: StationDetails | null,
  stations: StationSummary[],
  token: string | null,
): Promise<{ bikeId: string | null; error?: string }> {
  const value = input.trim();
  const compact = value.replace(/-/g, "").toLowerCase();
  if (isUuid(value)) {
    return { bikeId: value };
  }
  if (!isShortHex(compact)) {
    return { bikeId: null, error: "Enter a valid Bike ID (UUID or 8-char prefix)." };
  }
  // Prefer selected station details
  if (selectedStationId && stationDetails && stationDetails.stationId === selectedStationId) {
    const matches = stationDetails.docks
      .map((d) => d.bikeId)
      .filter((id): id is string => Boolean(id))
      .filter((id) => id.replace(/-/g, "").toLowerCase().startsWith(compact));
    if (matches.length === 1) return { bikeId: matches[0]! };
    if (matches.length > 1) return { bikeId: null, error: "Bike ID is ambiguous; enter full ID." };
  }
  // Search all stations if needed
  const found = new Set<string>();
  for (const s of stations) {
    try {
      const details = await apiRequest<StationDetails>(
        `/stations/${s.stationId}/details`,
        { token: token ?? undefined },
      );
      for (const d of details.docks) {
        if (d.bikeId && d.bikeId.replace(/-/g, "").toLowerCase().startsWith(compact)) {
          found.add(d.bikeId);
          if (found.size > 1) return { bikeId: null, error: "Bike ID is ambiguous; enter full ID." };
        }
      }
    } catch {
      // ignore
    }
  }
  if (found.size === 1) return { bikeId: Array.from(found)[0] };
  return { bikeId: null, error: "Bike ID not found; select a station or enter full ID." };
}

export default function DashboardPage() {
  const auth = useAuth();

  const storedActiveTrip = useMemo(() => readActiveTripFromStorage(auth.userId), [auth.userId]);

  const [stations, setStations] = useState<StationSummary[]>([]);

  const [loadingStations, setLoadingStations] = useState(false);

  const [stationsError, setStationsError] = useState<string | null>(null);

  const [feedback, setFeedback] = useState<string | null>(null);

  const [events, setEvents] = useState<DomainEventEntry[]>([]);

  const [selectedStationId, setSelectedStationId] = useState<string | null>(
    null,
  );

  const [reservationResult, setReservationResult] =
    useState<ReservationResponse | null>(null);

  const [reservationCountdown, setReservationCountdown] = useState<
    string | null
  >(null);

  const [tripResult, setTripResult] = useState<TripResponse | null>(
    storedActiveTrip,
  );

const [tripCompletion, setTripCompletion] =
    useState<TripCompletionResponse | null>(null);

  const [activeTripId, setActiveTripId] = useState<string | null>(
    storedActiveTrip?.tripId ?? null,
  );
  const [pendingRideAction, setPendingRideAction] = useState<RideAction>(null);

  // Removed standalone status form state; toggling is inline per-station

  // Removed standalone capacity form state

  const [moveBikeForm, setMoveBikeForm] = useState(defaultMoveBikeForm);

  const [stationDetails, setStationDetails] = useState<StationDetails | null>(
    null,
  );

  const [loadingStationDetails, setLoadingStationDetails] = useState(false);

  const [stationDetailsError, setStationDetailsError] = useState<string | null>(
    null,
  );

  const refreshEvents = useCallback(async () => {
    try {
      const data = await apiRequest<DomainEventEntry[]>("/public/events");

      setEvents(data.slice(0, 20));
    } catch (err) {
      console.error(err);
    }
  }, [auth.token]);

  const loadStationDetails = useCallback(
    async (stationId: string) => {
      setLoadingStationDetails(true);

      setStationDetailsError(null);

      try {
        const data = await apiRequest<StationDetails>(
          `/stations/${stationId}/details`,
          { token: auth.token },
        );

        setStationDetails(data);
      } catch (err) {
        setStationDetailsError(
          err instanceof Error
            ? err.message
            : "Unable to load station details.",
        );
      } finally {
        setLoadingStationDetails(false);
      }
    },
    [auth.token],
  );

  const statusColors = useMemo(
    () =>
      ({
        EMPTY: "#1d4ed8",
        OCCUPIED: "#22c55e",
        FULL: "#ef4444",
        OUT_OF_SERVICE: "#6b7280",
      }) as const,
    [],
  );

  const statusLegend = useMemo<ReadonlyArray<[keyof typeof statusColors, string]>>(
    () => [
      ["EMPTY", "empty"],
      ["OCCUPIED", "occupied"],
      ["FULL", "full"],
      ["OUT_OF_SERVICE", "out of service"],
    ],
    [],
  );

  const rideActionInFlight = pendingRideAction !== null;

  const markerSize = 72;

  useEffect(() => {
    if (!auth.token) {
      setEvents([]);

      return;
    }

    void refreshEvents();

    const interval = window.setInterval(() => {
      void refreshEvents();
    }, 5000);

    return () => {
      window.clearInterval(interval);
    };
  }, [auth.token, refreshEvents]);

  useEffect(() => {
    if (!selectedStationId) {
      setStationDetails(null);

      return;
    }

    void loadStationDetails(selectedStationId);
  }, [selectedStationId, loadStationDetails]);

  useEffect(() => {
    if (auth.token) {
      void loadStations();
    }

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth.token]);

  // Load active reservation on page load/refresh so UI reflects server state
  const loadActiveReservation = useCallback(async () => {
    if (!auth.token || !auth.userId || auth.role !== "RIDER") return;
    try {
      const data = await apiRequest<ReservationResponse | undefined>(
        `/reservations/active?riderId=${auth.userId}`,
        { token: auth.token },
      );
      setReservationResult(data ?? null);
    } catch {
      // If the API returns 204 or an error, show no active reservation
      setReservationResult(null);
    }
  }, [auth.token, auth.userId, auth.role]);

  useEffect(() => {
    void loadActiveReservation();
  }, [loadActiveReservation]);

  const loadStations = async () => {
    setLoadingStations(true);

    setStationsError(null);

    try {
      const data = await apiRequest<StationSummary[]>("/stations", {
        token: auth.token ?? undefined,
      });

      setStations(data);
    } catch (err) {
      setStationsError(
        err instanceof Error ? err.message : "Unable to load stations",
      );
    } finally {
      setLoadingStations(false);
    }
  };

  // Countdown for active reservation

  useEffect(() => {
    if (!reservationResult) {
      setReservationCountdown(null);

      return;
    }

    const expiresAt = new Date(reservationResult.expiresAt).getTime();

    const timer = setInterval(() => {
      const diffMs = expiresAt - Date.now();

      if (diffMs <= 0) {
        setReservationCountdown("expired");

        clearInterval(timer);
      } else {
        const totalSec = Math.floor(diffMs / 1000);

        const mm = Math.floor(totalSec / 60)
          .toString()
          .padStart(2, "0");

        const ss = (totalSec % 60).toString().padStart(2, "0");

        setReservationCountdown(`${mm}:${ss}`);
      }
    }, 1000);

    return () => clearInterval(timer);
  }, [reservationResult]);

  useEffect(() => {
    writeActiveTripToStorage(auth.userId, tripResult);
  }, [auth.userId, tripResult]);

  if (!auth.token || !auth.role || !auth.userId) {
    return (
      <main>
        <h1>Dashboard</h1>

        <p>You need to sign in to access ShareCycle operations.</p>
      </main>
    );
  }

  const reserveBike = async (
    stationId: string,
    bikeId: string | null,
    dockId?: string,
  ) => {
    if (!stationId || !bikeId) {
      return;
    }

    if (pendingRideAction) {
      return;
    }

    setFeedback(null);
    setReservationResult(null);
    setReservationCountdown(null);
    setPendingRideAction("reserve");

    try {
      const payload = {
        riderId: auth.userId,
        stationId,
        bikeId,
        expiresAfterMinutes: DEFAULT_RESERVATION_MINUTES,
      };
      const response = await apiRequest<ReservationResponse>("/reservations", {
        method: "POST",
        token: auth.token,
        body: JSON.stringify(payload),
      });

      setReservationResult(response);
      setTripResult(null);
      setFeedback("Reservation created successfully.");

      if (dockId) {
        setStationDetails((current) => {
          if (!current || current.stationId !== stationId) {
            return current;
          }
          const updatedDocks = current.docks.map((dock) =>
            dock.dockId === dockId
              ? { ...dock, status: "OCCUPIED" as const, bikeId }
              : dock,
          );
          return {
            ...current,
            docks: updatedDocks,
          };
        });
      }

      await loadStations();
      await refreshEvents();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Reservation failed");
    } finally {
      setPendingRideAction(null);
    }
  };

  const startTrip = async (
    stationId: string,
    bikeId: string | null,
    dockId?: string,
  ) => {
    if (!stationId || !bikeId) {
      return;
    }

    if (pendingRideAction) {
      return;
    }

    setFeedback(null);
    setPendingRideAction("start");

    try {
      const payload: Record<string, unknown> = {
        riderId: auth.userId,
        bikeId,
        stationId,
      };
      const response = await apiRequest<TripResponse>("/trips", {
        method: "POST",
        token: auth.token,
        body: JSON.stringify(payload),
      });

      setTripResult(response);
      setTripCompletion(null);
      setActiveTripId(response.tripId);
      setReservationResult(null);
      setReservationCountdown(null);
      setFeedback("Trip started.");

      if (dockId) {
        setStationDetails((current) => {
          if (!current || current.stationId !== stationId) {
            return current;
          }
          const updatedDocks = current.docks.map((dock) =>
            dock.dockId === dockId
              ? { ...dock, status: "EMPTY" as const, bikeId: null }
              : dock,
          );
          return {
            ...current,
            docks: updatedDocks,
            bikesDocked: Math.max(0, current.bikesDocked - 1),
            bikesAvailable: Math.max(0, current.bikesAvailable - 1),
            freeDocks: Math.min(
              current.capacity,
              current.freeDocks + 1,
            ),
          };
        });
        setStations((current) =>
          current.map((station) =>
            station.stationId === stationId
              ? {
                  ...station,
                  bikesDocked: Math.max(0, station.bikesDocked - 1),
                  bikesAvailable: Math.max(0, station.bikesAvailable - 1),
                  freeDocks: Math.min(station.capacity, station.freeDocks + 1),
                }
              : station,
          ),
        );
      }

      await loadStations();
      await refreshEvents();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to start trip");
    } finally {
      setPendingRideAction(null);
    }
  };

  const completeTrip = async (stationId: string) => {
    if (!activeTripId) {
      setFeedback("No active trip to complete.");
      return;
    }

    if (pendingRideAction) {
      return;
    }

    setFeedback(null);
    setPendingRideAction("end");

    try {
      const response = await apiRequest<TripCompletionResponse>(
        `/trips/${activeTripId}/end`,
        {
          method: "POST",
          token: auth.token,
          body: JSON.stringify({
            stationId,
          }),
        },
      );

      setTripCompletion(response);
      setTripResult(null);
      setActiveTripId(null);
      setFeedback("Trip completed.");

      await loadStations();
      await refreshEvents();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to end trip");
    } finally {
      setPendingRideAction(null);
    }
  };

  // Removed: inline toggle is wired in Station details section

  // Removed standalone capacity handler

  const handleMoveBike = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setFeedback(null);

    try {
      const resolvedStation = await resolveDestinationStationIdAsync(
        moveBikeForm.destinationId,
        selectedStationId,
        stationDetails,
        stations,
        auth.token,
      );
      if (!resolvedStation.stationId) {
        setFeedback(resolvedStation.error || "Invalid destination ID.");
        return;
      }

      const resolvedBike = await resolveBikeIdAsync(
        moveBikeForm.bikeId,
        selectedStationId,
        stationDetails,
        stations,
        auth.token,
      );
      if (!resolvedBike.bikeId) {
        setFeedback(resolvedBike.error || "Invalid Bike ID.");
        return;
      }

      await apiRequest<StationSummary[]>("/stations/move-bike", {
        method: "POST",

        token: auth.token,

        body: JSON.stringify({
          operatorId: auth.userId,

          bikeId: resolvedBike.bikeId,
          destinationStationId: resolvedStation.stationId,
        }),
      });

      setFeedback("Bike moved successfully.");

      setMoveBikeForm(defaultMoveBikeForm);

      await loadStations();

      await refreshEvents();

      if (selectedStationId) {
        await loadStationDetails(selectedStationId);
      }
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to move bike");
    }
  };

  return (
    <main>
      <section>
        <h2>City Map</h2>

        <div
          style={{
            display: "flex",

            gap: 16,

            alignItems: "center",

            flexWrap: "wrap",

            margin: "8px 0",

            fontSize: 16,
          }}
        >
          <strong>Legend:</strong>

          <div
            style={{
              display: "flex",
              gap: 16,
              alignItems: "center",
              flexWrap: "wrap",
            }}
          >
            {statusLegend.map(([category, label]) => (
              <span
                key={category}
                style={{ display: "inline-flex", alignItems: "center", gap: 6 }}
              >
                <span
                  style={{
                    width: 18,
                    height: 18,
                    borderRadius: 3,
                    background: statusColors[category],
                  }}
                ></span>

                {label}
              </span>
            ))}
          </div>
        </div>

        <Map defaultCenter={[45.508, -73.587]} defaultZoom={13} height={600}>
          {stations.map((s) => {
            const lat = Number.isFinite(s.latitude) ? s.latitude : 45.508;

            const lng = Number.isFinite(s.longitude) ? s.longitude : -73.587;

            const status = (s.status as keyof typeof statusColors) ?? "EMPTY";
            const statusLabel = (s.status ?? "").toLowerCase();
            const markerColor = statusColors[status] ?? "#6b7280";

            return (
              <PigeonMarker
                key={s.stationId}
                width={markerSize}
                anchor={[lat, lng]}
                onClick={() => setSelectedStationId(s.stationId)}
              >
                <div
                  role="button"
                  tabIndex={0}
                  onClick={() => setSelectedStationId(s.stationId)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedStationId(s.stationId);
                    }
                  }}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 12,
                    cursor: "pointer",
                    pointerEvents: "auto",
                  }}
                >
                  <span
                  aria-label={`Station status ${statusLabel}`}
                  title={`${s.name ?? "Station"} - ${statusLabel}`}
                    style={{
                      width: 40,

                      height: 40,

                      borderRadius: "50%",

                      background: markerColor,

                      border: "3px solid #fff",

                      boxShadow: "0 0 0 3px rgba(0,0,0,0.35)",
                    }}
                  />

                  <span
                    style={{
                      background: "rgba(255,255,255,0.9)",

                      padding: "6px 10px",

                      borderRadius: 8,

                      fontSize: 16,

                      fontWeight: 700,

                      color: "#222",
                    }}
                  >
                    {s.name ?? "Station"}
                  </span>
                </div>
              </PigeonMarker>
            );
          })}
        </Map>
      </section>

      <header>
        <h1>ShareCycle Dashboard</h1>

        <p>
          Signed in as <strong>{auth.username}</strong> (
          {auth.role.toLowerCase()}).
        </p>

        <button type="button" onClick={() => auth.logout()}>
          Logout
        </button>
      </header>

      <section>
        <h2>Station Overview</h2>

        {loadingStations && <p>Loading stations…</p>}

        {stationsError && <p role="alert">{stationsError}</p>}

        {!loadingStations && !stationsError && (
          <table>
            <thead>
              <tr>
                <th>Name</th>

                <th>Status</th>

                <th>Bikes available</th>

                <th>Bikes docked</th>

                <th>Free docks</th>

                <th>Capacity</th>

                <th>Details</th>
              </tr>
            </thead>

            <tbody>
              {stations.map((station) => (
                <tr key={station.stationId}>
                  <td>{station.name ?? "Unnamed station"}</td>

                  <td>{formatStationStatus(station.status)}</td>

                  <td>{station.bikesAvailable}</td>

                  <td>{station.bikesDocked}</td>

                  <td>{station.freeDocks}</td>

                  <td>{station.capacity}</td>

                  <td>
                    <button
                      type="button"
                      onClick={() => setSelectedStationId(station.stationId)}
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {auth.role === "RIDER" && (
        <section>
          <h2>My Ride</h2>
          <p>
            Pick a station from the map or the overview table to see available
            bikes. Actions now live directly beside each dock in the station
            details panel.
          </p>

          <div style={{ marginTop: 12 }}>
            <h3>Reservation</h3>
            {reservationResult ? (
              <p>
                Bike{" "}
                <strong>
                  {reservationResult.bikeId.slice(0, 8).toUpperCase()}
                </strong>{" "}
                at station{" "}
                <strong>
                  {reservationResult.stationId.slice(0, 8).toUpperCase()}
                </strong>{" "}
                {reservationCountdown === "expired"
                  ? "reservation expired."
                  : reservationCountdown
                    ? `expires in ${reservationCountdown}.`
                    : "reservation active."}
              </p>
            ) : (
              <p>No active reservations.</p>
            )}
            <p style={{ fontSize: 12, marginTop: 4 }}>
              Reservations hold a bike for {DEFAULT_RESERVATION_MINUTES} minutes.
            </p>
          </div>

          <div style={{ marginTop: 12 }}>
            <h3>Trip</h3>
            {activeTripId && tripResult ? (
              <p>
                Trip <strong>{tripResult.tripId.slice(0, 8).toUpperCase()}</strong>{" "}
                started at station{" "}
                <strong>
                  {tripResult.stationId.slice(0, 8).toUpperCase()}
                </strong>
                . Select a destination station and use “End trip here” in the
                details panel.
              </p>
            ) : (
              <p>No active trip.</p>
            )}
          </div>

          {tripCompletion && (
            <div style={{ marginTop: 12 }}>
              <h3>Last receipt</h3>
              <p>
                Trip {tripCompletion.tripId.slice(0, 8).toUpperCase()} ended at{" "}
                {new Date(tripCompletion.endedAt).toLocaleString()} - total $
                {tripCompletion.totalAmount.toFixed(2)}.
              </p>
            </div>
          )}

          {rideActionInFlight && (
            <p style={{ marginTop: 8, fontSize: 12 }}>
              Processing your ride request…
            </p>
          )}
        </section>
      )}

      {auth.role === "OPERATOR" && (
        <section>
          <h2>Operator Controls</h2>

          {/* Removed standalone toggle/capacity forms; use inline controls in Station details */}

          <form onSubmit={handleMoveBike}>
            <h3>Move a bike</h3>

            <label>
              Bike ID
              <input
                required
                value={moveBikeForm.bikeId}
                onChange={(event) =>
                  setMoveBikeForm((current) => ({
                    ...current,
                    bikeId: event.target.value,
                  }))
                }
              />
            </label>

            <label>
              Destination ID
              <input
                required
                value={moveBikeForm.destinationId}
                onChange={(event) =>
                  setMoveBikeForm((current) => ({
                    ...current,
                    destinationId: event.target.value,
                  }))
                }
              />
            </label>

            <button type="submit">Move bike</button>
          </form>
        </section>
      )}

      <section>
        <h2>Activity feedback</h2>

        {feedback && <p>{feedback}</p>}

        {reservationResult && (
          <p>
            Reservation {reservationResult.reservationId} valid until{" "}
            {new Date(reservationResult.expiresAt).toLocaleString()}.
            {reservationCountdown && reservationCountdown !== "expired" && (
              <span> Time left: {reservationCountdown}</span>
            )}
            {reservationCountdown === "expired" && <span> (expired)</span>}
          </p>
        )}

        {tripResult && (
          <p>
            Trip {tripResult.tripId} started at{" "}
            {new Date(tripResult.startedAt).toLocaleString()}.
          </p>
        )}

        {tripCompletion && (
          <p>
            Trip {tripCompletion.tripId} ended at{" "}
            {new Date(tripCompletion.endedAt).toLocaleString()}. Charge: $
            {tripCompletion.totalAmount.toFixed(2)}.
          </p>
        )}
      </section>

      <section>
        <h2>Station details</h2>

        {!selectedStationId && (
          <p>Select a station from the table to view details.</p>
        )}

        {selectedStationId && (
          <div>
            {(() => {
              const summary = stations.find(
                (st) => st.stationId === selectedStationId,
              );

              if (!summary) return <p>Station not found.</p>;

              const detailed =
                stationDetails && stationDetails.stationId === summary.stationId
                  ? stationDetails
                  : null;

              const docks = detailed?.docks ?? [];
              const stationIdForActions =
                detailed?.stationId ?? summary.stationId;

              const isRider = auth.role === "RIDER";
              const isOperator = auth.role === "OPERATOR";
              const reservationActive =
                !rideActionInFlight &&
                Boolean(reservationResult?.active) &&
                reservationCountdown !== "expired";
              const activeReservationBikeId =
                reservationActive && reservationResult?.bikeId
                  ? reservationResult?.bikeId
                  : null;
              const hasActiveTrip = Boolean(activeTripId);
              // Removed Move bike here button; use global form

              const canToggleStatus = isOperator;

              const canAdjustCapacity = isOperator;

              // Note: inline condition is used where this was referenced

              return (
                <div>
                  <p>
                    <strong>{summary.name ?? "Unnamed station"}</strong> -
                    Status: {formatStationStatus(summary.status)}
                  </p>
                  <p style={{ fontSize: 12, color: "#FFFFFF" }}>
                    Station ID: <code>{summary.stationId}</code>
                  </p>

                  <p>
                    Bikes available: {summary.bikesAvailable} | Docked: {" "}
                    {summary.bikesDocked} | Free docks: {summary.freeDocks} |
                    Capacity: {summary.capacity}
                  </p>

                  {loadingStationDetails && <p>Loading station details�</p>}

                  {stationDetailsError && (
                    <p role="alert">{stationDetailsError}</p>
                  )}

                  {docks.length > 0 && (
                    <div
                      style={{
                        display: "grid",

                        gridTemplateColumns:
                          "repeat(auto-fill, minmax(140px, 1fr))",

                        gap: 8,

                        margin: "12px 0",
                      }}
                    >
                      {docks.map((dock) => {
                        const bikeId = dock.bikeId ?? null;
                        const isBikeDocked =
                          (dock.status === "OCCUPIED" ||
                            dock.status === "RESERVED") && Boolean(bikeId);
                        const isReservedBike =
                          Boolean(
                            activeReservationBikeId &&
                              bikeId &&
                              activeReservationBikeId === bikeId,
                          );
                        const reserveDisabled =
                          !isBikeDocked ||
                          summary.status === "OUT_OF_SERVICE" ||
                          hasActiveTrip ||
                          (reservationActive && !isReservedBike) ||
                          isReservedBike ||
                          rideActionInFlight;
                        const startDisabled =
                          !Boolean(bikeId) ||
                          summary.status === "OUT_OF_SERVICE" ||
                          hasActiveTrip ||
                          (reservationActive && !isReservedBike) ||
                          rideActionInFlight;
                        const palette = isReservedBike
                          ? DOCK_BACKGROUND.RESERVED
                          : DOCK_BACKGROUND[dock.status] ??
                            DOCK_BACKGROUND.EMPTY;

                        return (
                          <div
                            key={dock.dockId}
                            style={{
                              padding: 12,
                              borderRadius: 10,
                              border: `2px solid ${palette.border}`,
                              background: palette.background,
                              color: "#111827",
                              boxShadow: "0 1px 2px rgba(15, 23, 42, 0.12)",
                            }}
                          >
                            <strong>{formatDockStatus(dock.status)}</strong>

                            <div style={{ fontSize: 12 }}>
                              Dock {dock.dockId.slice(0, 8)}
                              {bikeId && (
                                <div>Bike {bikeId.slice(0, 8)}</div>
                              )}
                            </div>

                            {isRider && isBikeDocked && (
                              <div
                                style={{
                                  marginTop: 8,
                                  display: "flex",

                                  gap: 8,

                                  flexWrap: "wrap",
                                }}
                              >
                                <button
                                  type="button"
                                  disabled={reserveDisabled}
                                  onClick={() =>
                                    reserveBike(
                                      stationIdForActions,
                                      bikeId,
                                      dock.dockId,
                                    )
                                  }
                                >
                                  {isReservedBike ? "Reserved" : "Reserve"}
                                </button>

                                <button
                                  type="button"
                                  disabled={startDisabled}
                                  onClick={() =>
                                    startTrip(
                                      stationIdForActions,
                                      bikeId,
                                      dock.dockId,
                                    )
                                  }
                                >
                                  Start trip
                                </button>
                              </div>
                            )}

                            {isReservedBike &&
                              reservationCountdown &&
                              reservationCountdown !== "expired" && (
                                <div style={{ marginTop: 6, fontSize: 12 }}>
                                  Expires in {reservationCountdown}
                                </div>
                              )}
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {isOperator && (
                    <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                      <button
                        type="button"
                        disabled={!canToggleStatus}
                        onClick={async () => {
                          try {
                            await apiRequest<StationSummary>(
                              `/stations/${summary.stationId}/status`,
                              {
                                method: "PATCH",
                                token: auth.token,
                                body: JSON.stringify({
                                  operatorId: auth.userId,
                                  outOfService:
                                    summary.status !== "OUT_OF_SERVICE",
                                }),
                              },
                            );
                            setFeedback("Station status updated.");
                            await loadStations();
                            await refreshEvents();
                            await loadStationDetails(summary.stationId);
                          } catch (err) {
                            setFeedback(
                              err instanceof Error
                                ? err.message
                                : "Unable to update station status",
                            );
                          }
                        }}
                      >
                        Toggle Status
                      </button>

                      <button
                        type="button"
                        disabled={!canAdjustCapacity}
                        onClick={() =>
                          (async () => {
                            try {
                              await apiRequest<StationSummary>(
                                `/stations/${summary.stationId}/capacity`,
                                {
                                  method: "PATCH",
                                  token: auth.token,
                                  body: JSON.stringify({
                                    operatorId: auth.userId,
                                    delta: 1,
                                  }),
                                },
                              );
                              setFeedback("Station capacity updated.");
                              await loadStations();
                              await refreshEvents();
                              await loadStationDetails(summary.stationId);
                            } catch (err) {
                              setFeedback(
                                err instanceof Error
                                  ? err.message
                                  : "Unable to update capacity",
                              );
                            }
                          })()
                        }
                      >
                        +1 Dock
                      </button>

                      <button
                        type="button"
                        disabled={!canAdjustCapacity}
                        onClick={() =>
                          (async () => {
                            try {
                              await apiRequest<StationSummary>(
                                `/stations/${summary.stationId}/capacity`,
                                {
                                  method: "PATCH",
                                  token: auth.token,
                                  body: JSON.stringify({
                                    operatorId: auth.userId,
                                    delta: -1,
                                  }),
                                },
                              );
                              setFeedback("Station capacity updated.");
                              await loadStations();
                              await refreshEvents();
                              await loadStationDetails(summary.stationId);
                            } catch (err) {
                              setFeedback(
                                err instanceof Error
                                  ? err.message
                                  : "Unable to update capacity",
                              );
                            }
                          })()
                        }
                      >
                        -1 Dock
                      </button>

                      {/* Removed: use global Move a bike form */}
                    </div>
                  )}

                  {isRider && hasActiveTrip && (
                    <div
                      style={{
                        display: "flex",
                        gap: 8,
                        flexWrap: "wrap",
                        marginTop: 12,
                      }}
                      >
                      <button
                        type="button"
                        disabled={
                          summary.status === "OUT_OF_SERVICE" ||
                          summary.freeDocks === 0 ||
                          rideActionInFlight
                        }
                        onClick={() => completeTrip(stationIdForActions)}
                      >
                        End trip here
                      </button>

                        {(summary.status === "OUT_OF_SERVICE" ||
                        summary.freeDocks === 0) && (
                      <span style={{ alignSelf: "center", fontSize: 12 }}>
                        {summary.status === "OUT_OF_SERVICE"
                          ? "Station is out of service."
                          : "No free docks available."}
                      </span>
                    )}
                    {rideActionInFlight && (
                      <span style={{ alignSelf: "center", fontSize: 12 }}>
                        Finishing previous request…
                      </span>
                    )}
                  </div>
                )}
                </div>
              );
            })()}
          </div>
        )}
      </section>

      <section>
        <h2>Event console</h2>

        {events.length === 0 && <p>No events yet.</p>}

        {events.length > 0 && (
          <ol>
            {events.map((event) => (
              <li key={`${event.type}-${event.occurredAt}`}>
                [{new Date(event.occurredAt).toLocaleTimeString()}] {event.type}{" "}
                � {event.payload}
              </li>
            ))}
          </ol>
        )}
      </section>
    </main>
  );
}

