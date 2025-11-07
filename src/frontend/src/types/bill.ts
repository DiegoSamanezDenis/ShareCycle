export interface Bill {
  billId: string;
  computedAt: string;
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
}

export interface BillIssuedEvent {
  tripId: string;
  riderId: string;
  billId: string;
  ledgerId: string;
  computedAt: string;
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
  pricingPlan: string;
}
