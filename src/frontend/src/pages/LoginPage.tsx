import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import AppShell from "../components/layout/AppShell";
import PageSection from "../components/layout/PageSection";

type LoginResponse = {
  userId: string;
  username: string;
  role: "RIDER" | "OPERATOR";
  token: string;
};

export default function LoginPage() {
  const navigate = useNavigate();
  const auth = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await apiRequest<LoginResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      auth.login({
        token: response.token,
        role: response.role,
        userId: response.userId,
        username: response.username,
      });
      navigate("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  };

  const heroActions = (
    <Link
      to="/register"
      style={{
        borderRadius: 999,
        padding: "0.6rem 1.4rem",
        border: "1px solid var(--border)",
        fontWeight: 600,
        color: "var(--text)",
      }}
    >
      Create account
    </Link>
  );

  return (
    <AppShell
      heading="Welcome back"
      subheading="Enter your credentials to manage reservations, trips, and operator tools."
      actions={heroActions}
    >
      <PageSection title="Sign in">
        <form
          onSubmit={handleSubmit}
          noValidate
          style={{ display: "grid", gap: "1rem", maxWidth: 420 }}
        >
          <label>
            Username
            <input
              required
              name="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              disabled={submitting}
              autoComplete="username"
            />
          </label>
          <label>
            Password
            <input
              required
              type="password"
              name="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={submitting}
              autoComplete="current-password"
            />
          </label>
          <button type="submit" disabled={submitting}>
            {submitting ? "Signing in…" : "Login"}
          </button>
          {error && (
            <p role="alert" style={{ margin: 0, color: "var(--danger)" }}>
              {error}
            </p>
          )}
        </form>
        <p style={{ marginTop: "1.5rem" }}>
          New to ShareCycle? <Link to="/register">Create an account</Link>.
        </p>
      </PageSection>
    </AppShell>
  );
}
