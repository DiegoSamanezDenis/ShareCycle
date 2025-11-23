import { useEffect, useMemo, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { apiRequest } from "../api/client";
import type { PricingPlan } from "../types/pricing";
import AppShell from "../components/layout/AppShell";
import PageSection from "../components/layout/PageSection";

type RegisterFormState = {
  fullName: string;
  streetAddress: string;
  email: string;
  username: string;
  password: string;
  paymentMethodToken: string;
  pricingPlanType: string;
};

const defaultFormState: RegisterFormState = {
  fullName: "",
  streetAddress: "",
  email: "",
  username: "",
  password: "",
  paymentMethodToken: "",
  pricingPlanType: "",
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [formState, setFormState] =
    useState<RegisterFormState>(defaultFormState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [pricingPlans, setPricingPlans] = useState<PricingPlan[]>([]);
  const [plansLoading, setPlansLoading] = useState(false);
  const [planLoadError, setPlanLoadError] = useState<string | null>(null);

  useEffect(() => {
    const loadPlans = async () => {
      setPlansLoading(true);
      try {
        const data = await apiRequest<PricingPlan[]>("/pricing", {
          skipAuth: true,
        });
        setPricingPlans(data);
        setPlanLoadError(null);
        setFormState((current) => {
          if (current.pricingPlanType) {
            return current;
          }
          const planParam = searchParams.get("plan")?.trim().toUpperCase();
          const initialPlan =
            data.find((plan) => plan.planType === planParam) ?? data[0];
          return {
            ...current,
            pricingPlanType: initialPlan?.planType ?? "",
          };
        });
      } catch {
        setPlanLoadError(
          "Unable to load pricing plans right now. Please try again soon.",
        );
      } finally {
        setPlansLoading(false);
      }
    };

    void loadPlans();
  }, [searchParams]);

  const selectedPlan = useMemo(
    () =>
      pricingPlans.find(
        (plan) => plan.planType === formState.pricingPlanType,
      ) ?? null,
    [pricingPlans, formState.pricingPlanType],
  );

  const handleChange = (
    event: ChangeEvent<HTMLInputElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target;
    setFormState((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(false);

    if (!formState.pricingPlanType) {
      setError("Please choose a pricing plan before registering.");
      setSubmitting(false);
      return;
    }

    try {
      await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName: formState.fullName,
          streetAddress: formState.streetAddress,
          email: formState.email,
          username: formState.username,
          password: formState.password,
          paymentMethodToken: formState.paymentMethodToken,
          pricingPlanType: formState.pricingPlanType,
        }),
      });
      setSuccess(true);
      setFormState((current) => ({
        ...defaultFormState,
        pricingPlanType: current.pricingPlanType,
      }));
      setTimeout(() => navigate("/login"), 1200);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed");
    } finally {
      setSubmitting(false);
    }
  };

  const heroActions = (
    <Link
      to="/login"
      style={{
        borderRadius: 999,
        padding: "0.6rem 1.4rem",
        border: "1px solid var(--border)",
        fontWeight: 600,
        color: "var(--text)",
      }}
    >
      Back to sign in
    </Link>
  );

  return (
    <AppShell
      heading="Create a rider account"
      subheading="Choose a pricing plan, add your billing token, and start riding immediately."
      actions={heroActions}
    >
      <PageSection title="Your details">
        <form
          onSubmit={handleSubmit}
          noValidate
          style={{ display: "grid", gap: "1rem", maxWidth: 520 }}
        >
          <label>
            Full name
            <input
              required
              name="fullName"
              value={formState.fullName}
              onChange={handleChange}
              disabled={submitting}
            />
          </label>
          <label>
            Street address
            <input
              required
              name="streetAddress"
              value={formState.streetAddress}
              onChange={handleChange}
              disabled={submitting}
            />
          </label>
          <label>
            Email
            <input
              required
              type="email"
              name="email"
              value={formState.email}
              onChange={handleChange}
              disabled={submitting}
            />
          </label>
          <label>
            Username
            <input
              required
              name="username"
              value={formState.username}
              onChange={handleChange}
              disabled={submitting}
            />
          </label>
          <label>
            Password
            <input
              required
              type="password"
              name="password"
              value={formState.password}
              onChange={handleChange}
              disabled={submitting}
              minLength={8}
            />
          </label>
          <label>
            Payment method token
            <input
              required
              name="paymentMethodToken"
              value={formState.paymentMethodToken}
              onChange={handleChange}
              disabled={submitting}
              placeholder="e.g. tok_123"
            />
          </label>
          <label>
            Pricing plan
            <select
              required
              name="pricingPlanType"
              value={formState.pricingPlanType}
              onChange={handleChange}
              disabled={submitting || plansLoading || pricingPlans.length === 0}
            >
              <option value="" disabled>
                {plansLoading ? "Loading plans..." : "Select a plan"}
              </option>
              {pricingPlans.map((plan) => {
                const label =
                  plan.subscriptionFee !== null
                    ? `${plan.name} — $${plan.subscriptionFee.toFixed(2)}/month`
                    : `${plan.name} — $${plan.baseCost.toFixed(2)} base`;
                return (
                  <option key={plan.planId} value={plan.planType}>
                    {label}
                  </option>
                );
              })}
            </select>
          </label>
          {planLoadError && (
            <p role="alert" style={{ color: "var(--danger)", margin: 0 }}>
              {planLoadError}
            </p>
          )}
          {selectedPlan && (
            <div
              style={{
                background: "var(--surface-muted)",
                padding: "1rem",
                borderRadius: 14,
                border: "1px solid var(--border)",
                fontSize: 14,
                lineHeight: 1.5,
                color: "var(--text)",
              }}
            >
              <strong>{selectedPlan.name}</strong>
              {selectedPlan.description && (
                <span>: {selectedPlan.description}</span>
              )}
              <div style={{ marginTop: 8 }}>
                {selectedPlan.subscriptionFee !== null && (
                  <>
                    Subscription: ${selectedPlan.subscriptionFee.toFixed(2)} per
                    month ·{" "}
                  </>
                )}
                Per-trip base cost:{" "}
                {selectedPlan.baseCost > 0
                  ? `$${selectedPlan.baseCost.toFixed(2)}`
                  : selectedPlan.subscriptionFee !== null
                  ? "Included"
                  : "None (pay per minute)"}{" "}
                · Per minute:{" "}
                {selectedPlan.perMinuteRate > 0
                  ? `$${selectedPlan.perMinuteRate.toFixed(2)}/min`
                  : selectedPlan.subscriptionFee !== null
                  ? "Included"
                  : "Included"}
                {" · "}
                E-bike surcharge:{" "}
                {selectedPlan.eBikeSurchargePerMinute &&
                selectedPlan.eBikeSurchargePerMinute > 0
                  ? `$${selectedPlan.eBikeSurchargePerMinute.toFixed(2)}/min`
                  : selectedPlan.subscriptionFee !== null
                  ? "Included"
                  : "None"}
              </div>
              <div style={{ marginTop: 6 }}>
                Example {selectedPlan.sample.durationMinutes}-minute ride:
                <div>
                  • Standard:{" "}
                  {selectedPlan.sample.standardBikeCost > 0
                    ? `$${selectedPlan.sample.standardBikeCost.toFixed(2)}`
                    : selectedPlan.subscriptionFee !== null
                    ? "Included"
                    : "$0.00"}
                </div>
                <div>
                  • E-bike:{" "}
                  {selectedPlan.sample.eBikeCost > 0
                    ? `$${selectedPlan.sample.eBikeCost.toFixed(2)}`
                    : selectedPlan.subscriptionFee !== null
                    ? "Included"
                    : "$0.00"}
                </div>
              </div>
              {selectedPlan.subscriptionFee !== null && (
                <p style={{ marginTop: 6, color: "var(--brand)" }}>
                  Your monthly subscription covers every ride—no additional trip
                  charges.
                </p>
              )}
            </div>
          )}
          <button type="submit" disabled={submitting || !formState.pricingPlanType}>
            {submitting ? "Submitting…" : "Register"}
          </button>
          {error && (
            <p role="alert" style={{ color: "var(--danger)", margin: 0 }}>
              {error}
            </p>
          )}
          {success && <p>Registration successful! Redirecting to login…</p>}
        </form>
        {!success && (
          <p style={{ marginTop: "1.5rem" }}>
            Already have an account? <Link to="/login">Sign in</Link>.
          </p>
        )}
      </PageSection>
    </AppShell>
  );
}
