import { apiRequest } from "./client";
import type { TripHistoryPage, TripDetails, BikeType } from "../types/trip";

export type TripHistoryFilters = {
  startTime?: string;
  endTime?: string;
  bikeType?: BikeType | null;
  tripId?: string | null;
};

type PaginationOptions = {
  page?: number;
  pageSize?: number;
};

function buildHistoryQuery(filters: TripHistoryFilters, options?: PaginationOptions): string {
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
  if (filters.tripId) {
    params.set("tripId", filters.tripId);
  }
  if (typeof options?.page === "number") {
    params.set("page", String(Math.max(0, options.page)));
  }
  if (typeof options?.pageSize === "number") {
    params.set("pageSize", String(Math.max(1, options.pageSize)));
  }
  const query = params.toString();
  return query ? `?${query}` : "";
}

export async function fetchTripHistory(
  filters: TripHistoryFilters,
  token?: string | null,
  options?: PaginationOptions,
): Promise<TripHistoryPage> {
  const normalizedFilters: TripHistoryFilters = {
    ...filters,
    tripId: filters.tripId?.trim() ? filters.tripId.trim() : undefined,
  };
  const query = buildHistoryQuery(normalizedFilters, options);
  return apiRequest<TripHistoryPage>(`/trips${query}`, {
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

