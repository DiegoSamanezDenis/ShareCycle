import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiRequest } from '../api/client';

type RegisterFormState = {
  fullName: string;
  streetAddress: string;
  email: string;
  username: string;
  password: string;
  paymentMethodToken: string;
};

const defaultFormState: RegisterFormState = {
  fullName: '',
  streetAddress: '',
  email: '',
  username: '',
  password: '',
  paymentMethodToken: ''
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const [formState, setFormState] = useState<RegisterFormState>(defaultFormState);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const handleChange = (event: ChangeEvent<HTMLInputElement>) => {
    const { name, value } = event.target;
    setFormState((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(false);

    try {
      await apiRequest('/auth/register', {
        method: 'POST',
        body: JSON.stringify({
          fullName: formState.fullName,
          streetAddress: formState.streetAddress,
          email: formState.email,
          username: formState.username,
          password: formState.password,
          paymentMethodToken: formState.paymentMethodToken
        })
      });
      setSuccess(true);
      setFormState(defaultFormState);
      setTimeout(() => navigate('/login'), 1200);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main>
      <h1>Create Rider Account</h1>
      <p>Provide your information to access ShareCycle as a rider.</p>
      <form onSubmit={handleSubmit} noValidate>
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
        <button type="submit" disabled={submitting}>
          {submitting ? 'Submitting…' : 'Register'}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {success && <p>Registration successful! Redirecting to login…</p>}
    </main>
  );
}
