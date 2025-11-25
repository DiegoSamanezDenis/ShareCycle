export type BikeType = "STANDARD" | "E_BIKE";

export type LedgerStatus = "PENDING" | "PAID" | null;

export type TripHistoryEntry = {
  tripId: string;
  riderId: string | null;
  riderName: string | null;
  startStationName: string | null;
  endStationName: string | null;
  startTime: string | null;
  endTime: string | null;
  durationMinutes: number;
  bikeType: BikeType | null;
  bikeId: string | null;
  totalCost: number;
  ledgerId: string | null;
  ledgerStatus: LedgerStatus;
};

export type TripHistoryPage = {
  entries: TripHistoryEntry[];
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
};

export type TripBillBreakdown = {
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
};

export type TripDetails = {
  tripId: string;
  riderId: string | null;
  riderName: string | null;
  startStationName: string | null;
  endStationName: string | null;
  startTime: string | null;
  endTime: string | null;
  durationMinutes: number;
  bikeId: string | null;
  bikeType: BikeType | null;
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
  ledgerId: string | null;
  ledgerStatus: "PENDING" | "PAID" | null;
};

