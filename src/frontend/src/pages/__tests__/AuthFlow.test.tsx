import { describe, expect, it, beforeEach, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { routes } from '../../routes';
import { AuthProvider } from '../../auth/AuthContext';
import LoginPage from '../../pages/LoginPage';

vi.mock('../../api/client', () => {
  return {
    apiRequest: vi.fn(async (path: string, opts?: any) => {
      if (path === '/auth/login' && opts?.method === 'POST') {
        return {
          token: 'demo-token',
          role: 'RIDER',
          userId: 'u1',
          username: 'rider1'
        };
      }
      if (path === '/stations') {
        return [];
      }
      return undefined;
    })
  };
});

function renderWithProviders(initialEntries: string[]) {
  const router = createMemoryRouter(routes, { initialEntries });
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  );
}

describe('Auth guard', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('shows login page when accessing dashboard without token', () => {
    renderWithProviders(['/dashboard']);
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('renders dashboard when token exists', async () => {
    localStorage.setItem('sharecycle.auth', JSON.stringify({ token: 't', role: 'RIDER', userId: 'u1', username: 'r' }));
    renderWithProviders(['/dashboard']);
    expect(await screen.findByRole('heading', { name: /Station Overview/i })).toBeInTheDocument();
  });
});

describe('Login flow', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('logs in and navigates to dashboard', async () => {
    const router = createMemoryRouter(routes, { initialEntries: ['/login'] });
    render(
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    );
    fireEvent.change(screen.getByLabelText(/Username/i), { target: { value: 'rider1' } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: 'password123' } });
    fireEvent.click(screen.getByRole('button', { name: /login/i }));
    // After login, router should navigate to dashboard and render Station Overview
    expect(await screen.findByRole('heading', { name: /Station Overview/i })).toBeInTheDocument();
  });
});


