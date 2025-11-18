import { useCallback, useEffect, useMemo, useState } from "react";
import type { FormEvent } from "react";
import { Map, Marker as PigeonMarker } from "pigeon-maps";

import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import EventConsole from "../components/EventConsole";
import RideHistory from "../components/RideHistory";
import styles from "./TripReceipt.module.css";
import type { StationDetails, StationSummary } from "../types/station";
import type { LedgerStatus } from "../types/trip";
import { payLedger } from "../api/payments";
import LoyaltyBadge from "../components/LoyaltyBadge";

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

type CourtesyCredit = {
  ledgerId: string;
  amount: number;
  description: string | null;
};

type ReturnSuggestion = {
  stationId: string;
  name: string | null;
  freeDocks: number;
  distanceMeters: number;
};

type PaymentStatus = "PAID" | "PENDING" | "NOT_REQUIRED" | "UNKNOWN";

type TripCompletionSuccess = {
  status: "COMPLETED";
  tripId: string;
  stationId: string | null;
  endedAt: string;
  durationMinutes: number;
  ledgerId: string;
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
  ledgerStatus: LedgerStatus | null;
  paymentStatus: PaymentStatus;
  message: string;
};

type TripCompletionBlocked = {
  status: "BLOCKED";
  tripId: string;
  stationId: string;
  message: string;
  credit?: CourtesyCredit | null;
  suggestions: ReturnSuggestion[];
  ledgerStatus?: LedgerStatus | null;
  paymentStatus?: PaymentStatus | null;
};

type TripCompletionResponse = TripCompletionSuccess | TripCompletionBlocked;

type MoveBikeFormState = {
  bikeId: string;
  destinationId: string;
};

const DEFAULT_RESERVATION_MINUTES = 5;

const DOCK_BACKGROUND: Record<
  string,
  { background: string; border: string }
> = Object.freeze({
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
    const scopedKey = getActiveTripKeyForUser(userId);
    let raw = window.localStorage.getItem(scopedKey);
    if (!raw) {
      const legacy = window.localStorage.getItem(ACTIVE_TRIP_STORAGE_KEY);
      if (legacy) {
        try {
          const legacyParsed = JSON.parse(legacy) as Partial<TripResponse>;
          if (legacyParsed && legacyParsed.riderId && legacyParsed.riderId === userId) {
            window.localStorage.setItem(scopedKey, legacy);
          }
        } finally {
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
  const scopedKey = getActiveTripKeyForUser(userId);
  if (trip) {
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
    window.localStorage.removeItem(scopedKey);
  }
}

function formatDistance(meters: number): string {
  if (!Number.isFinite(meters) || meters < 0) {
    return "N/A";
  }
  if (meters < 1000) {
    return `${Math.round(meters)} m`;
  }
  return `${(meters / 1000).toFixed(1)} km`;
}

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

function formatPaymentStatus(status?: PaymentStatus | null): string {
  switch (status) {
    case "PAID":
      return "Paid";
    case "PENDING":
      return "Pending";
    case "NOT_REQUIRED":
      return "Not required";
    case "UNKNOWN":
    default:
      return "Unknown";
  }
}

function isUuid(value: string): boolean {
  return /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/.test(
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
    const matches = stationDetails.docks.filter((dock) => {
      const dockCompact = dock.dockId.replace(/-/g, "").toLowerCase();
      const bikeCompact = dock.bikeId ? dock.bikeId.replace(/-/g, "").toLowerCase() : "";
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
  const station = stations.find((summary) => summary.stationId === value);
  if (station) {
    return { stationId: station.stationId };
  }
  const stationMatches = stations.filter((summary) =>
    summary.stationId.replace(/-/g, "").toLowerCase().startsWith(compact),
  );
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
  const quick = resolveDestinationStationIdFromInput(
    input,
    selectedStationId,
    stationDetails,
    stations,
  );
  if (quick.stationId || isUuid(input.trim())) {
    return quick;
  }
  const compact = input.trim().replace(/-/g, "").toLowerCase();
  if (!isShortHex(compact)) {
    return quick;
  }
  const matches: string[] = [];
  for (const summary of stations) {
    try {
      const details = await apiRequest<StationDetails>(
        `/stations/${summary.stationId}/details`,
        { token: token ?? undefined },
      );
      const has = details.docks.some((dock) =>
        dock.dockId.replace(/-/g, "").toLowerCase().startsWith(compact),
      );
      if (has) {
        matches.push(summary.stationId);
        if (matches.length > 1) {
          return { stationId: null, error: "Dock ID matches multiple stations; enter full ID." };
        }
      }
    } catch {
      // ignore failures while scanning other stations
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
  if (selectedStationId && stationDetails && stationDetails.stationId === selectedStationId) {
    const matches = stationDetails.docks
      .map((dock) => dock.bikeId)
      .filter((id): id is string => Boolean(id))
      .filter((id) => id.replace(/-/g, "").toLowerCase().startsWith(compact));
    if (matches.length === 1) {
      return { bikeId: matches[0]! };
    }
    if (matches.length > 1) {
      return { bikeId: null, error: "Bike ID is ambiguous; enter full ID." };
    }
  }
  const found = new Set<string>();
  for (const summary of stations) {
    try {
      const details = await apiRequest<StationDetails>(
        `/stations/${summary.stationId}/details`,
        { token: token ?? undefined },
      );
      for (const dock of details.docks) {
        if (dock.bikeId && dock.bikeId.replace(/-/g, "").toLowerCase().startsWith(compact)) {
          found.add(dock.bikeId);
          if (found.size > 1) {
            return { bikeId: null, error: "Bike ID is ambiguous; enter full ID." };
          }
        }
      }
    } catch {
      // ignore failures while scanning other stations
    }
  }
  if (found.size === 1) {
    return { bikeId: Array.from(found)[0] };
  }
  return { bikeId: null, error: "Bike ID not found; select a station or enter full ID." };
}

export default function DashboardPage() {
  const auth = useAuth();

  const storedActiveTrip = useMemo(() => readActiveTripFromStorage(auth.userId), [auth.userId]);

  const [stations, setStations] = useState<StationSummary[]>([]);
  const [loadingStations, setLoadingStations] = useState(false);
  const [stationsError, setStationsError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [selectedStationId, setSelectedStationId] = useState<string | null>(null);
  const [reservationResult, setReservationResult] = useState<ReservationResponse | null>(null);
  const [reservationCountdown, setReservationCountdown] = useState<string | null>(null);
  const [tripResult, setTripResult] = useState<TripResponse | null>(storedActiveTrip);
  const [tripCompletion, setTripCompletion] = useState<TripCompletionSuccess | null>(null);
  const [payingLedgerId, setPayingLedgerId] = useState<string | null>(null);
  const [returnBlock, setReturnBlock] = useState<TripCompletionBlocked | null>(null);
  const [activeTripId, setActiveTripId] = useState<string | null>(storedActiveTrip?.tripId ?? null);
  const [pendingRideAction, setPendingRideAction] = useState<RideAction>(null);
  const [moveBikeForm, setMoveBikeForm] = useState(defaultMoveBikeForm);
  const [stationDetails, setStationDetails] = useState<StationDetails | null>(null);
  const [loadingStationDetails, setLoadingStationDetails] = useState(false);
  const [stationDetailsError, setStationDetailsError] = useState<string | null>(null);
  const [credit, setCredit] = useState<number>(0); 
  

  const loadStationDetails = useCallback(
    async (stationId: string) => {
      setLoadingStationDetails(true);
      setStationDetailsError(null);
      try {
        const details = await apiRequest<StationDetails>(
          `/stations/${stationId}/details`,
          { token: auth.token },
        );
        setStationDetails(details);
      } catch (err) {
        setStationDetailsError(
          err instanceof Error ? err.message : "Unable to load station details.",
        );
      } finally {
        setLoadingStationDetails(false);
      }
    },
    [auth.token],
  );

  const loadStations = useCallback(async () => {
    if (!auth.token) {
      setStations([]);
      return;
    }
    setLoadingStations(true);
    setStationsError(null);
    try {
      const summaries = await apiRequest<StationSummary[]>("/stations", {
        token: auth.token ?? undefined,
      });
      setStations(summaries);
    } catch (err) {
      setStationsError(err instanceof Error ? err.message : "Unable to load stations");
    } finally {
      setLoadingStations(false);
    }
  }, [auth.token]);

  const loadActiveReservation = useCallback(async () => {
    if (!auth.token || !auth.userId || auth.role !== "RIDER") {
      setReservationResult(null);
      return;
    }
    try {
      const data = await apiRequest<ReservationResponse | undefined>(
        `/reservations/active?riderId=${auth.userId}`,
        { token: auth.token },
      );
      setReservationResult(data ?? null);
    } catch {
      setReservationResult(null);
    }
  }, [auth.role, auth.token, auth.userId]);

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
    if (!selectedStationId) {
      setStationDetails(null);
      return;
    }
    void loadStationDetails(selectedStationId);
  }, [loadStationDetails, selectedStationId]);

  useEffect(() => {
    if (auth.token) {
      void loadStations();
    } else {
      setStations([]);
    }
  }, [auth.token, loadStations]);

  useEffect(() => {
    void loadActiveReservation();
  }, [loadActiveReservation]);

  useEffect(() => {
    if (!reservationResult) {
      setReservationCountdown(null);
      return;
    }
    const expiresAt = new Date(reservationResult.expiresAt).getTime();
    const timer = window.setInterval(() => {
      const diffMs = expiresAt - Date.now();
      if (diffMs <= 0) {
        setReservationCountdown("expired");
        window.clearInterval(timer);
        return;
      }
      const totalSeconds = Math.floor(diffMs / 1000);
      const mm = Math.floor(totalSeconds / 60)
        .toString()
        .padStart(2, "0");
      const ss = (totalSeconds % 60).toString().padStart(2, "0");
      setReservationCountdown(`${mm}:${ss}`);
    }, 1000);
    return () => window.clearInterval(timer);
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

  const reserveBike = async (stationId: string, bikeId: string | null, dockId?: string) => {
    if (!stationId || !bikeId || pendingRideAction) {
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
          const docks = current.docks.map((dock) =>
            dock.dockId === dockId ? { ...dock, status: "OCCUPIED" as const, bikeId } : dock,
          );
          return { ...current, docks };
        });
      }
      await loadStations();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Reservation failed");
    } finally {
      setPendingRideAction(null);
    }
  };

  const startTrip = async (stationId: string, bikeId: string | null, dockId?: string) => {
    if (!stationId || !bikeId || pendingRideAction) {
      return;
    }
    setFeedback(null);
    setPendingRideAction("start");
    try {
      const payload = {
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
      setReturnBlock(null);
      setActiveTripId(response.tripId);
      setReservationResult(null);
      setReservationCountdown(null);
      setFeedback("Trip started.");
      if (dockId) {
        setStationDetails((current) => {
          if (!current || current.stationId !== stationId) {
            return current;
          }
          const docks = current.docks.map((dock) =>
            dock.dockId === dockId ? { ...dock, status: "EMPTY" as const, bikeId: null } : dock,
          );
          return {
            ...current,
            docks,
            bikesDocked: Math.max(0, current.bikesDocked - 1),
            bikesAvailable: Math.max(0, current.bikesAvailable - 1),
            freeDocks: Math.min(current.capacity, current.freeDocks + 1),
          };
        });
        setStations((current) =>
          current.map((summary) =>
            summary.stationId === stationId
              ? {
                  ...summary,
                  bikesDocked: Math.max(0, summary.bikesDocked - 1),
                  bikesAvailable: Math.max(0, summary.bikesAvailable - 1),
                  freeDocks: Math.min(summary.capacity, summary.freeDocks + 1),
                }
              : summary,
          ),
        );
      }
      await loadStations();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to start trip");
    } finally {
      setPendingRideAction(null);
    }
  };

  const completeTrip = async (stationId: string) => {
    if (!activeTripId || pendingRideAction) {
      if (!activeTripId) {
        setFeedback("No active trip to complete.");
      }
      return;
    }
    setFeedback(null);
    setPendingRideAction("end");
    try {
      const response = await apiRequest<TripCompletionResponse>(`/trips/${activeTripId}/end`, {
        method: "POST",
        token: auth.token,
        body: JSON.stringify({ stationId }),
      });
      if (response.status === "COMPLETED") {
        const creditResponse = await apiRequest<{ amount: number }>(`/auth/credit?userId=${auth.userId}`, {
          method: "GET",
          token: auth.token,
        });
        console.log("Credit after trip completion:", creditResponse.amount);
        setCredit(creditResponse.amount); // save credit to state
        setTripCompletion(response);
        setReturnBlock(null);
        setTripResult(null);
        setActiveTripId(null);
        setFeedback(response.message ?? "Trip completed.");
      } else {
        setTripCompletion(null);
        setReturnBlock(response);
        setFeedback(response.message || "Destination station is full.");
      }
      await loadStations();
      await loadStationDetails(stationId);
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to end trip");
    } finally {
      setPendingRideAction(null);
    }
  };

  const settleLedger = async (ledgerId: string) => {
    if (!ledgerId) return;
    setPayingLedgerId(ledgerId);
    setFeedback(null);
    try {
      const response = await payLedger(ledgerId, auth.token);
      setTripCompletion((current) => {
        if (!current) return current;
        if (current.ledgerId !== ledgerId) {
          return current;
        }
        return {
          ...current,
          ledgerStatus: response.ledgerStatus ?? current.ledgerStatus,
          paymentStatus: response.paymentStatus ?? current.paymentStatus,
        };
      });
      setFeedback("Payment processed successfully.");
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to process payment.");
    } finally {
      setPayingLedgerId(null);
    }
  };

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
      if (selectedStationId) {
        await loadStationDetails(selectedStationId);
      }
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : "Unable to move bike");
    }
  };

  return (
    <main>
      <header>
        <h1>ShareCycle Dashboard</h1>
        <p>
          Signed in as <strong>{auth.username}</strong> ({auth.role.toLowerCase()}).
          Current Credit: <strong>${credit.toFixed(2)}</strong>
        </p>
        <button type="button" onClick={() => auth.logout()}>
          Logout
        </button>
      </header>

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
                />
                {label}
              </span>
            ))}
          </div>
        </div>
        <Map
          defaultCenter={[45.508, -73.587]}
          defaultZoom={13}
          height={600}
          provider={(x: number, y: number, z: number) =>
            `https://a.tile.openstreetmap.org/${z}/${x}/${y}.png`
          }
        >
          {stations.map((summary) => {
            const lat = Number.isFinite(summary.latitude) ? summary.latitude : 45.508;
            const lng = Number.isFinite(summary.longitude) ? summary.longitude : -73.587;
            const status = (summary.status as keyof typeof statusColors) ?? "EMPTY";
            const markerColor = statusColors[status] ?? "#6b7280";
            const statusLabel = (summary.status ?? "").toLowerCase();
            return (
              <PigeonMarker
                key={summary.stationId}
                width={markerSize}
                anchor={[lat, lng]}
                onClick={() => setSelectedStationId(summary.stationId)}
              >
                <div
                  role="button"
                  tabIndex={0}
                  onClick={() => setSelectedStationId(summary.stationId)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      setSelectedStationId(summary.stationId);
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
                    title={`${summary.name ?? "Station"} - ${statusLabel}`}
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
                    {summary.name ?? "Station"}
                  </span>
                </div>
              </PigeonMarker>
            );
          })}
        </Map>
      </section>

      <section>
        <h2>Station Overview</h2>
        {loadingStations && <p>Loading stations...</p>}
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
              {stations.map((summary) => (
                <tr key={summary.stationId}>
                  <td>{summary.name ?? "Unnamed station"}</td>
                  <td>{formatStationStatus(summary.status)}</td>
                  <td>{summary.bikesAvailable}</td>
                  <td>{summary.bikesDocked}</td>
                  <td>{summary.freeDocks}</td>
                  <td>{summary.capacity}</td>
                  <td>
                    <button
                      type="button"
                      onClick={() => setSelectedStationId(summary.stationId)}
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
          <LoyaltyBadge userId={auth.userId} token={auth.token} />
          <p>
            Pick a station from the map or the overview table to see available bikes. Actions
            are listed beside each dock in the station details panel.
          </p>
          <div style={{ marginTop: 12 }}>
            <h3>Reservation</h3>
            {reservationResult ? (
              <p>
                Bike{" "}
                <strong>{reservationResult.bikeId.slice(0, 8).toUpperCase()}</strong> at station{" "}
                <strong>{reservationResult.stationId.slice(0, 8).toUpperCase()}</strong>{" "}
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
                Trip <strong>{tripResult.tripId.slice(0, 8).toUpperCase()}</strong> started at
                station{" "}
                <strong>{tripResult.stationId.slice(0, 8).toUpperCase()}</strong>. Select a
                destination station and use "End trip here" in the details panel.
              </p>
            ) : (
              <p>No active trip.</p>
            )}
          </div>
          {tripCompletion && (
            <div style={{ marginTop: 12 }}>
              <h3>Last Trip Receipt</h3>
              <div className={styles.receiptCard}>
                <div className={styles.receiptRow}>
                  <span className={styles.receiptLabel}>Trip ID:</span>
                  <span className={styles.receiptValue}>
                    {tripCompletion.tripId.slice(0, 8).toUpperCase()}
                  </span>
                </div>
                <div className={styles.receiptRow}>
                  <span className={styles.receiptLabel}>Ended at:</span>
                  <span className={styles.receiptValue}>
                    {new Date(tripCompletion.endedAt).toLocaleString()}
                  </span>
                </div>
                <div className={styles.receiptRow}>
                  <span className={styles.receiptLabel}>Duration:</span>
                  <span className={styles.receiptValue}>
                    {tripCompletion.durationMinutes} minutes
                  </span>
                </div>
                <div className={styles.billBreakdown}>
                  <div className={styles.billBreakdownTitle}>Bill Breakdown</div>
                  <div className={styles.billItem}>
                    <span className={styles.billItemLabel}>Base Cost:</span>
                    <span className={styles.billItemValue}>
                      ${tripCompletion.baseCost.toFixed(2)}
                    </span>
                  </div>
                  <div className={styles.billItem}>
                    <span className={styles.billItemLabel}>Time Cost:</span>
                    <span className={styles.billItemValue}>
                      ${tripCompletion.timeCost.toFixed(2)}
                    </span>
                  </div>
                  {tripCompletion.eBikeSurcharge > 0 && (
                    <div className={styles.billItem}>
                      <span className={styles.billItemLabel}>E-Bike Surcharge:</span>
                      <span className={styles.billItemValue}>
                        ${tripCompletion.eBikeSurcharge.toFixed(2)}
                      </span>
                    </div>
                  )}
                </div>
                <div className={styles.totalRow}>
                  <span>Total:</span>
                  <span>${tripCompletion.totalCost.toFixed(2)}</span>
                </div>
                <div className={styles.receiptRow}>
                  <span className={styles.receiptLabel}>Payment:</span>
                  <span className={styles.receiptValue}>
                    {formatPaymentStatus(tripCompletion.paymentStatus)}
                  </span>
                </div>
                {tripCompletion.paymentStatus === "PENDING" && tripCompletion.ledgerId && (
                  <button
                    type="button"
                    className={styles.button}
                    onClick={() => settleLedger(tripCompletion.ledgerId)}
                    disabled={payingLedgerId === tripCompletion.ledgerId}
                    style={{ marginTop: 12 }}
                  >
                    {payingLedgerId === tripCompletion.ledgerId ? "Processing..." : "Pay now"}
                  </button>
                )}
              </div>
            </div>
          )}
          {rideActionInFlight && (
            <p style={{ marginTop: 8, fontSize: 12 }}>Processing your ride request...</p>
          )}
        </section>
      )}

      {auth.role === "OPERATOR" && (
        <section>
          <h2>Operator Controls</h2>
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
            Trip {tripResult.tripId} started at {new Date(tripResult.startedAt).toLocaleString()}.
          </p>
        )}
        {tripCompletion && (
          <div className={styles.feedbackReceipt}>
            <div className={styles.feedbackTitle}>Trip Completed</div>
            <div className={styles.feedbackRow}>
              Trip {tripCompletion.tripId.slice(0, 8).toUpperCase()} ended at{" "}
              {new Date(tripCompletion.endedAt).toLocaleString()}
            </div>
            <div className={styles.feedbackRow}>
              Duration: {tripCompletion.durationMinutes} minutes
            </div>
            <div className={styles.feedbackRow}>
              Base: ${tripCompletion.baseCost.toFixed(2)} + Time: $
              {tripCompletion.timeCost.toFixed(2)}
              {tripCompletion.eBikeSurcharge > 0 && (
                <span> + E-Bike: ${tripCompletion.eBikeSurcharge.toFixed(2)}</span>
              )}
            </div>
            <div className={styles.feedbackTotal}>
              Total Charge: ${tripCompletion.totalCost.toFixed(2)}
            </div>
            <div className={styles.feedbackTotal}>
              Available Flex Credit: ${credit.toFixed(2)}
            </div>
            <div className={styles.feedbackRow}>
              Payment: {formatPaymentStatus(tripCompletion.paymentStatus)}
            </div>
            {tripCompletion.paymentStatus === "PENDING" && tripCompletion.ledgerId && (
              <button
                type="button"
                className={styles.button}
                onClick={() => settleLedger(tripCompletion.ledgerId)}
                disabled={payingLedgerId === tripCompletion.ledgerId}
              >
                {payingLedgerId === tripCompletion.ledgerId ? "Processing..." : "Pay now"}
              </button>
            )}
          </div>
        )}
        {returnBlock && (
          <div className={styles.blockedReturn}>
            <div className={styles.blockedTitle}>Return Blocked</div>
            <p className={styles.blockedMessage}>{returnBlock.message}</p>
            {returnBlock.credit && (
              <p className={styles.blockedMessage}>
                Credit applied: ${returnBlock.credit.amount.toFixed(2)} —{" "}
                {returnBlock.credit.description ?? "Courtesy credit"}
              </p>
            )}
            {returnBlock.suggestions.length > 0 && (
              <div className={styles.blockedSuggestions}>
                <p>Nearby stations with free docks:</p>
                <ul>
                  {returnBlock.suggestions.map((suggestion) => (
                    <li key={suggestion.stationId}>
                      <strong>{suggestion.name ?? suggestion.stationId.slice(0, 8).toUpperCase()}</strong>
                      <span>
                        {" "}
                        • {suggestion.freeDocks} dock
                        {suggestion.freeDocks === 1 ? "" : "s"} free •{" "}
                        {formatDistance(suggestion.distanceMeters)}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </section>

      <section>
        <h2>Station details</h2>
        {!selectedStationId && <p>Select a station from the table to view details.</p>}
        {selectedStationId && (
          <div>
            {(() => {
              const summary = stations.find((station) => station.stationId === selectedStationId);
              if (!summary) {
                return <p>Station not found.</p>;
              }
              const detailed =
                stationDetails && stationDetails.stationId === summary.stationId
                  ? stationDetails
                  : null;
              const docks = detailed?.docks ?? [];
              const stationIdForActions = detailed?.stationId ?? summary.stationId;
              const isRider = auth.role === "RIDER";
              const isOperator = auth.role === "OPERATOR";
              const reservationActive =
                !rideActionInFlight &&
                Boolean(reservationResult?.active) &&
                reservationCountdown !== "expired";
              const activeReservationBikeId =
                reservationActive && reservationResult?.bikeId ? reservationResult.bikeId : null;
              const hasActiveTrip = Boolean(activeTripId);
              const canToggleStatus = isOperator;
              const canAdjustCapacity = isOperator;

              return (
                <div>
                  <p>
                    <strong>{summary.name ?? "Unnamed station"}</strong> - Status:{" "}
                    {formatStationStatus(summary.status)}
                  </p>
                  <p style={{ fontSize: 12 }}>
                    Station ID: <code>{summary.stationId}</code>
                  </p>
                  <p>
                    Bikes available: {summary.bikesAvailable} | Docked: {summary.bikesDocked} |
                    Free docks: {summary.freeDocks} | Capacity: {summary.capacity}
                  </p>
                  {loadingStationDetails && <p>Loading station details...</p>}
                  {stationDetailsError && <p role="alert">{stationDetailsError}</p>}

                  {docks.length > 0 && (
                    <div
                      style={{
                        display: "grid",
                        gridTemplateColumns: "repeat(auto-fill, minmax(140px, 1fr))",
                        gap: 8,
                        margin: "12px 0",
                      }}
                    >
                      {docks.map((dock) => {
                        const bikeId = dock.bikeId ?? null;
                        const isBikeDocked =
                          (dock.status === "OCCUPIED" || dock.status === "RESERVED") &&
                          Boolean(bikeId);
                        const isReservedBike =
                          Boolean(activeReservationBikeId && bikeId === activeReservationBikeId);
                        const reserveDisabled =
                          !isBikeDocked ||
                          summary.status === "OUT_OF_SERVICE" ||
                          hasActiveTrip ||
                          (reservationActive && !isReservedBike) ||
                          isReservedBike ||
                          rideActionInFlight;
                        const startDisabled =
                          !bikeId ||
                          summary.status === "OUT_OF_SERVICE" ||
                          hasActiveTrip ||
                          (reservationActive && !isReservedBike) ||
                          rideActionInFlight;
                        const palette = isReservedBike
                          ? DOCK_BACKGROUND.RESERVED
                          : DOCK_BACKGROUND[dock.status] ?? DOCK_BACKGROUND.EMPTY;

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
                              {bikeId && <div>Bike {bikeId.slice(0, 8)}</div>}
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
                                    reserveBike(stationIdForActions, bikeId, dock.dockId)
                                  }
                                >
                                  {isReservedBike ? "Reserved" : "Reserve"}
                                </button>
                                <button
                                  type="button"
                                  disabled={startDisabled}
                                  onClick={() =>
                                    startTrip(stationIdForActions, bikeId, dock.dockId)
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
                            await apiRequest<StationSummary>(`/stations/${summary.stationId}/status`, {
                              method: "PATCH",
                              token: auth.token,
                              body: JSON.stringify({
                                operatorId: auth.userId,
                                outOfService: summary.status !== "OUT_OF_SERVICE",
                              }),
                            });
                            setFeedback("Station status updated.");
                            await loadStations();
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
                        disabled={summary.status === "OUT_OF_SERVICE" || rideActionInFlight}
                        onClick={() => completeTrip(stationIdForActions)}
                        style={
                          summary.status === "OUT_OF_SERVICE"
                            ? undefined
                            : summary.freeDocks === 0
                              ? { borderColor: "#f97316", color: "#f97316" }
                              : undefined
                        }
                      >
                        End trip here
                      </button>
                      {summary.status === "OUT_OF_SERVICE" && (
                        <span style={{ alignSelf: "center", fontSize: 12 }}>
                          Station is out of service.
                        </span>
                      )}
                      {summary.freeDocks === 0 && summary.status !== "OUT_OF_SERVICE" && (
                        <span style={{ alignSelf: "center", fontSize: 12, color: "#f97316" }}>
                          No free docks available; ending here will offer alternatives.
                        </span>
                      )}
                      {rideActionInFlight && (
                        <span style={{ alignSelf: "center", fontSize: 12 }}>
                          Finishing previous request...
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

      {auth.role === "OPERATOR" ? (
        <section>
          <h2>Ride history</h2>
          <p>Billing history is available to riders only.</p>
        </section>
      ) : (
        <RideHistory token={auth.token} isOperator={false} />
      )}

      <section>
        <h2>Event console</h2>
        <EventConsole token={auth.token} />
      </section>
    </main>
  );
}
