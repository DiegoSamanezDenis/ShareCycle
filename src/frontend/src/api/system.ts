import { apiRequest } from "./client";

export type ResetSummary = {
  bikes: number;
  stations: number;
  docks: number;
};

export async function resetSystem(token?: string | null): Promise<ResetSummary> {
  return apiRequest<ResetSummary>("/system/reset", {
    method: "POST",
    token,
  });
}

