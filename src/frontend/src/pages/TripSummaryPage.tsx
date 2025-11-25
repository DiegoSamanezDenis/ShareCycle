//src/pages/TripSummaryPage.tsx
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { apiRequest } from "../api/client";
import type { LedgerStatus } from "../types/trip";
import { payLedger } from "../api/payments";
import AppShell from "../components/layout/AppShell";
import PageSection from "../components/layout/PageSection";
import RideHistory from "../components/RideHistory";

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
  discountRate: number;
  discountAmount: number;
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
  const [credit, setCredit] = useState<number>(0);

  function formatErrorMessage(rawMessage: string | null): string {
    if (!rawMessage) {
      return "Unable to load trip summary right now.";
    }
    const normalized = rawMessage.toLowerCase();
    if (normalized.includes("not_found") || normalized.includes("no completed trips")) {
      return "No completed trips found.";
    }
    if (normalized.includes("forbidden") || normalized.includes("riders only")) {
      return "Trip summary is only available in rider mode.";
    }
    return "Unable to load trip summary right now.";
  }

  useEffect(() => {
    if (!auth.token) return;

    const fetchTripSummary = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await apiRequest<TripSummaryResponse>("/trips/last-completed", {
          token: auth.token,
        });
        const creditResponse = await apiRequest<{ amount: number }>(`/auth/credit?userId=${auth.userId}`, {
          method: "GET",
          token: auth.token,
        });
        setCredit(creditResponse.amount);
        setTripSummary(response);
      } catch (err) {
        const message = err instanceof Error ? err.message : null;
        setError(formatErrorMessage(message));
        setTripSummary(null);
      } finally {
        setLoading(false);
      }
    };

    void fetchTripSummary();
  }, [auth.token, auth.userId]);

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

  const heroActions = !auth.token ? (
    <Link
      to="/login"
      style={{
        borderRadius: 999,
        padding: "0.6rem 1.4rem",
        background: "var(--brand)",
        color: "#fff",
        fontWeight: 600,
      }}
    >
      Sign in
    </Link>
  ) : undefined;

  return (
    <AppShell
      heading="Trip Summary"
      subheading="Review your last completed ride, pay outstanding balances, and confirm ledger status."
      actions={heroActions}
    >
      {!auth.token ? (
        <PageSection>
          <p>You need to sign in to view your trip summary.</p>
        </PageSection>
      ) : (
        <>
          <PageSection title="Latest receipt">
            {loading && <p>Loading trip summary…</p>}
            {error && !loading && <p role="alert">{error}</p>}
            {!loading && !error && !tripSummary && <p>No completed trips found.</p>}
            {!loading && !error && tripSummary && (
              <>
                <div
                  style={{
                    display: "grid",
                    gap: "0.35rem",
                    marginBottom: "1rem",
                    fontSize: "0.95rem",
                  }}
                >
                  <div>
                    Trip{" "}
                    <strong>{tripSummary.tripId.slice(0, 8).toUpperCase()}</strong> ended at{" "}
                    {new Date(tripSummary.endedAt).toLocaleString()}.
                  </div>
                  <div>Duration: {tripSummary.durationMinutes} minutes.</div>
                  <div>
                    Ledger ID:{" "}
                    {tripSummary.ledgerId
                      ? tripSummary.ledgerId.slice(0, 8).toUpperCase()
                      : "N/A"}
                  </div>
                  <div>Ledger status: {tripSummary.ledgerStatus ?? "PENDING"}</div>
                </div>

                <div
                  style={{
                    border: "1px solid var(--border)",
                    borderRadius: 16,
                    padding: "1rem",
                    background: "var(--surface-muted)",
                  }}
                >
                  <h3 style={{ marginBottom: "0.75rem" }}>Charges</h3>
                  <div
                    style={{
                      display: "grid",
                      gap: "0.4rem",
                      fontSize: "0.95rem",
                    }}
                  >
                    <div>Base cost: ${tripSummary.baseCost.toFixed(2)}</div>
                    {tripSummary.discountRate > 0 && (
                      <div>
                        Loyalty discount ({Math.round(tripSummary.discountRate * 100)}%): -$
                        {tripSummary.discountAmount.toFixed(2)}
                      </div>
                    )}
                    <div>Time cost: ${tripSummary.timeCost.toFixed(2)}</div>
                    {tripSummary.eBikeSurcharge > 0 && (
                      <div>E-bike surcharge: ${tripSummary.eBikeSurcharge.toFixed(2)}</div>
                    )}
                    <div>Total: ${tripSummary.totalCost.toFixed(2)}</div>
                    <div>Available flex credit: ${credit.toFixed(2)}</div>
                    <div style={{ fontWeight: 600 }}>
                      Amount to pay: ${(tripSummary.totalCost - credit).toFixed(2)}
                    </div>
                  </div>
                </div>

                <div style={{ marginTop: "1rem" }}>
                  <div>Payment: {formatPaymentStatus(tripSummary.paymentStatus)}</div>
                  {tripSummary.paymentStatus === "PENDING" && tripSummary.ledgerId && (
                    <button
                      type="button"
                      onClick={handlePay}
                      disabled={paying}
                      style={{ marginTop: "0.5rem" }}
                    >
                      {paying ? "Processing..." : "Pay now"}
                    </button>
                  )}
                  {paymentMessage && <p style={{ marginTop: "0.5rem" }}>{paymentMessage}</p>}
                </div>
              </>
            )}
          </PageSection>

          <PageSection
            title="Ride history"
            description="Filter, search, and inspect past trips."
          >
            <RideHistory token={auth.token} isOperator={auth.effectiveRole === "OPERATOR"} />
          </PageSection>
        </>
      )}
    </AppShell>
  );
}
