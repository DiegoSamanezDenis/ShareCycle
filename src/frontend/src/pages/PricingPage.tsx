import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiRequest } from "../api/client";
import type { PricingPlan } from "../types/pricing";
import styles from "./PricingPage.module.css";

export default function PricingPage() {
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const loadPlans = async () => {
      setLoading(true);
      try {
        const data = await apiRequest<PricingPlan[]>("/pricing", {
          skipAuth: true,
        });
        setPlans(data);
        setError(null);
      } catch (err) {
        setError("Failed to load pricing information. Please try again later.");
        console.error("Error fetching pricing info:", err);
      } finally {
        setLoading(false);
      }
    };

    void loadPlans();
  }, []);

  const handleChoosePlan = (plan: PricingPlan) => {
    navigate(`/register?plan=${plan.planType}`);
  };

  if (error) {
    return <div className={styles.error}>{error}</div>;
  }

  if (loading) {
    return <div className={styles.loading}>Loading pricing information...</div>;
  }

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Pricing Plans</h1>
      <p className={styles.subtitle}>
        Transparent pricing for every type of rider. Pick the option that best matches how often you ride.
      </p>
      <div className={styles.grid}>
        {plans.map((plan) => (
          <div key={plan.planId} className={styles.card}>
            <h2 className={styles.cardTitle}>{plan.name}</h2>
            {plan.description && (
              <p className={styles.cardDescription}>{plan.description}</p>
            )}
            <dl className={styles.details}>
              {plan.subscriptionFee !== null && (
                <div>
                  <dt>Monthly subscription</dt>
                  <dd>${plan.subscriptionFee.toFixed(2)}</dd>
                </div>
              )}
              <div>
                <dt>Base cost (per trip)</dt>
                <dd>
                  {plan.baseCost > 0
                    ? `$${plan.baseCost.toFixed(2)}`
                    : plan.subscriptionFee !== null
                    ? "Included in subscription"
                    : "None (pay per minute)"}
                </dd>
              </div>
              <div>
                <dt>Per-minute rate</dt>
                <dd>
                  {plan.perMinuteRate > 0
                    ? `$${plan.perMinuteRate.toFixed(2)} / min`
                    : plan.subscriptionFee !== null
                    ? "Included"
                    : "Included after base cost"}
                </dd>
              </div>
              <div>
                <dt>E-bike surcharge</dt>
                <dd>
                  {plan.eBikeSurchargePerMinute && plan.eBikeSurchargePerMinute > 0
                    ? `$${plan.eBikeSurchargePerMinute.toFixed(2)} / min`
                    : plan.subscriptionFee !== null
                    ? "Included"
                    : "None"}
                </dd>
              </div>
            </dl>
            <div className={styles.sample}>
              <h3>Example Trip</h3>
              <p>
                • {plan.sample.durationMinutes}-minute standard bike ride:{" "}
                <strong>
                  {plan.sample.standardBikeCost > 0
                    ? `$${plan.sample.standardBikeCost.toFixed(2)}`
                    : plan.subscriptionFee !== null
                    ? "Included"
                    : "$0.00"}
                </strong>
              </p>
              <p>
                • {plan.sample.durationMinutes}-minute e-bike ride:{" "}
                <strong>
                  {plan.sample.eBikeCost > 0
                    ? `$${plan.sample.eBikeCost.toFixed(2)}`
                    : plan.subscriptionFee !== null
                    ? "Included"
                    : "$0.00"}
                </strong>
              </p>
            </div>
            {plan.subscriptionFee !== null && (
              <p className={styles.subscriptionNote}>
                All rides are included in your monthly subscription&mdash;no per-trip charges.
              </p>
            )}
            <button
              type="button"
              className={styles.button}
              onClick={() => handleChoosePlan(plan)}
            >
              Choose {plan.name}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
