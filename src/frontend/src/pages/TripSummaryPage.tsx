//src/pages/TripSummaryPage.tsx
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { apiRequest } from "../api/client";
import type { LedgerStatus } from "../types/trip";
import { payLedger } from "../api/payments";

type PaymentStatus = "PAID" | "PENDING" | "NOT_REQUIRED" | "UNKNOWN";

type TripSummaryResponse = {
  tripId: string;
  endStationId: string | null;
  endedAt: string;
  durationMinutes: number;
  ledgerId: string | null;
  baseCost: number;
  timeCost: number;
  eBikeSurcharge: number;
  totalCost: number;
  ledgerStatus: LedgerStatus | null;
  paymentStatus: PaymentStatus;
};

function formatPaymentStatus(status: PaymentStatus): string {
  switch (status) {
    case "PAID":
      return "Payment succeeded";
    case "PENDING":
      return "Payment pending";
    case "NOT_REQUIRED":
      return "Payment not required";
    case "UNKNOWN":
    default:
      return "Payment status unknown";
  }
}

export default function TripSummaryPage() {
  const auth = useAuth();
  const [tripSummary, setTripSummary] = useState<TripSummaryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [paymentMessage, setPaymentMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!auth.token) return;

    const fetchTripSummary = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiRequest<TripSummaryResponse>("/trips/last-completed", {
          token: auth.token,
        });
        setTripSummary(response);
      } catch (err) {
        const message = err instanceof Error ? err.message : "Unable to load trip summary.";
        setError(message);
        setTripSummary(null);
      } finally {
        setLoading(false);
      }
    };

    void fetchTripSummary();
  }, [auth.token]);

  const handlePay = async () => {
    if (!tripSummary?.ledgerId) return;
    setPaying(true);
    setPaymentMessage(null);
    try {
      const result = await payLedger(tripSummary.ledgerId, auth.token);
      setTripSummary((current) =>
        current
          ? {
              ...current,
              ledgerStatus: result.ledgerStatus ?? current.ledgerStatus,
              paymentStatus: result.paymentStatus ?? current.paymentStatus,
            }
          : current,
      );
      setPaymentMessage("Payment processed successfully.");
    } catch (err) {
      setPaymentMessage(err instanceof Error ? err.message : "Unable to process payment.");
    } finally {
      setPaying(false);
    }
  };

  if (!auth.token) {
    return <p>You need to sign in to view trip summary.</p>;
  }

  if (loading) return <p>Loading trip summary…</p>;

  if (error) return <p role="alert">{error}</p>;

  if (!tripSummary) return <p>No completed trips found.</p>;

  return (
    <main>
      <h1>Trip Summary</h1>

      <p>
        Trip <strong>{tripSummary.tripId.slice(0, 8).toUpperCase()}</strong> ended at{" "}
        {new Date(tripSummary.endedAt).toLocaleString()}.
      </p>
      <p>Duration: {tripSummary.durationMinutes} minutes.</p>
      <p>Ledger ID: {tripSummary.ledgerId ? tripSummary.ledgerId.slice(0, 8).toUpperCase() : "—"}</p>
      <p>Ledger status: {tripSummary.ledgerStatus ?? "PENDING"}</p>

      <section>
        <h2>Charges</h2>
        <ul>
          <li>Base cost: ${tripSummary.baseCost.toFixed(2)}</li>
          <li>Time cost: ${tripSummary.timeCost.toFixed(2)}</li>
          <li>E-Bike surcharge: ${tripSummary.eBikeSurcharge.toFixed(2)}</li>
          <li>
            <strong>Total: ${tripSummary.totalCost.toFixed(2)}</strong>
          </li>
        </ul>
      </section>

      <p>Payment: {formatPaymentStatus(tripSummary.paymentStatus)}</p>
      {tripSummary.paymentStatus === "PENDING" && tripSummary.ledgerId && (
        <button type="button" onClick={handlePay} disabled={paying} style={{ marginTop: "0.5rem" }}>
          {paying ? "Processing..." : "Pay now"}
        </button>
      )}
      {paymentMessage && <p>{paymentMessage}</p>}
    </main>
  );
}
