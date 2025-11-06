//src/pages/TripSummaryPage.tsx
import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { apiRequest } from "../api/client";

type TripCompletionResponse = {
  tripId: string;
  endStationId: string;
  endedAt: string;
  durationMinutes: number;
  ledgerId: string;
  totalAmount: number;
};

export default function TripSummaryPage() {
  const auth = useAuth();
  const [tripCompletion, setTripCompletion] = useState<TripCompletionResponse | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!auth.token) return;

    const fetchTripSummary = async () => {
      try {
        const response = await apiRequest<TripCompletionResponse>(
          `/trips/last-completed?riderId=${auth.userId}`,
          { token: auth.token }
        );

        setTripCompletion(response);

        // Simulate payment status check
        setPaymentStatus("success"); // or "failure"
      } catch (err) {
        setPaymentStatus("failure");
      } finally {
        setLoading(false);
      }
    };

    void fetchTripSummary();
  }, [auth.token, auth.userId]);

  if (!auth.token) {
    return <p>You need to sign in to view trip summary.</p>;
  }

  if (loading) return <p>Loading trip summary…</p>;

  if (!tripCompletion) return <p>No completed trips found.</p>;

  return (
    <main>
      <h1>Trip Summary</h1>

      <p>
        Trip <strong>{tripCompletion.tripId.slice(0, 8).toUpperCase()}</strong> ended at{" "}
        {new Date(tripCompletion.endedAt).toLocaleString()}.
      </p>

      <p>Total amount: ${tripCompletion.totalAmount.toFixed(2)}</p>

      {paymentStatus === "success" && <p style={{ color: "green" }}>Payment Successful </p>}
      {paymentStatus === "failure" && <p style={{ color: "red" }}>Payment Failed </p>}
    </main>
  );
}
