export type PricingPlanType = "PAY_AS_YOU_GO" | "MONTHLY_SUBSCRIBER";

export interface PricingPlanSample {
  durationMinutes: number;
  standardBikeCost: number;
  eBikeCost: number;
}

export interface PricingPlan {
  planId: string;
  name: string;
  description: string | null;
  planType: PricingPlanType;
  baseCost: number;
  perMinuteRate: number;
  eBikeSurchargePerMinute: number | null;
  subscriptionFee: number | null;
  sample: PricingPlanSample;
}
