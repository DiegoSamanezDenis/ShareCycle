import { apiRequest } from "./client";
import type { TripHistoryEntry, TripDetails, BikeType } from "../types/trip";

export type TripHistoryFilters = {
  startTime?: string;
  endTime?: string;
  bikeType?: BikeType | null;
};

function buildHistoryQuery(filters: TripHistoryFilters): string {
  const params = new URLSearchParams();
  if (filters.startTime) {
    params.set("startTime", filters.startTime);
  }
  if (filters.endTime) {
    params.set("endTime", filters.endTime);
  }
  if (filters.bikeType) {
    params.set("bikeType", filters.bikeType);
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function fetchTripHistory(
  filters: TripHistoryFilters,
  token?: string | null,
): Promise<TripHistoryEntry[]> {
  const query = buildHistoryQuery(filters);
  return apiRequest<TripHistoryEntry[]>(`/trips${query}`, {
    token: token ?? undefined,
  });
}

export async function fetchTripDetails(
  tripId: string,
  token?: string | null,
): Promise<TripDetails> {
  return apiRequest<TripDetails>(`/trips/${tripId}`, {
    token: token ?? undefined,
  });
}

