import { apiRequest } from "./client";
import type { LedgerStatus } from "../types/trip";

export type PaymentResult = {
  ledgerId: string;
  ledgerStatus: LedgerStatus;
  totalCost: number;
  paymentStatus: "PAID" | "PENDING" | "NOT_REQUIRED";
};

export async function payLedger(ledgerId: string, token?: string | null): Promise<PaymentResult> {
  return apiRequest<PaymentResult>(`/trips/ledger/${ledgerId}/pay`, {
    method: "POST",
    token: token ?? undefined,
  });
}
