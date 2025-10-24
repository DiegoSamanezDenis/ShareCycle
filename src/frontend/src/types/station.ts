export type FullnessCategory = "EMPTY" | "LOW" | "HEALTHY" | "FULL" | "UNKNOWN";

export type StationSummary = {
  stationId: string;
  name: string | null;
  status: "EMPTY" | "OCCUPIED" | "FULL" | "OUT_OF_SERVICE";
  bikesAvailable: number;
  bikesDocked: number;
  capacity: number;
  freeDocks: number;
  latitude: number;
  longitude: number;
  fullnessCategory: FullnessCategory;
};

export type DockSummary = {
  dockId: string;
  status: "EMPTY" | "OCCUPIED" | "RESERVED" | "OUT_OF_SERVICE";
  bikeId: string | null;
};

export type StationDetails = StationSummary & {
  docks: DockSummary[];
};
