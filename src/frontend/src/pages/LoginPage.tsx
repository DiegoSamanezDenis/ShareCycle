import { useState } from "react";
import type { FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthContext";

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

  return (
    <main>
      <h1>Sign in</h1>
      <p>Enter your credentials to access the ShareCycle dashboard.</p>
      <form onSubmit={handleSubmit} noValidate>
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
      </form>
      {error && <p role="alert">{error}</p>}
      <p>
        Don't have an account? <Link to="/register">Create one</Link>.
      </p>
    </main>
  );
}
