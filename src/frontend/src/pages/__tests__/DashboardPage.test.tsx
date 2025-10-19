import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import DashboardPage from '../../pages/DashboardPage';
import { AuthProvider } from '../../auth/AuthContext';

vi.mock('../../api/client', () => {
  return {
    apiRequest: vi.fn(async (path: string, opts?: any) => {
      if (path === '/stations' && (!opts || opts.method === undefined)) {
        return [
          {
            stationId: 's1',
            name: 'Station #1',
            status: 'OCCUPIED',
            bikesDocked: 3,
            capacity: 5,
            freeDocks: 2
          }
        ];
      }
      return undefined;
    })
  };
});

function renderWithAuth(ui: React.ReactElement) {
  // Seed localStorage for AuthContext
  localStorage.setItem(
    'sharecycle.auth',
    JSON.stringify({ token: 'demo', role: 'RIDER', userId: 'u1', username: 'rider1' })
  );
  return render(<AuthProvider>{ui}</AuthProvider>);
}

describe('DashboardPage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('renders station overview', async () => {
    renderWithAuth(<DashboardPage />);
    // Wait for overview to load
    const overviewHeading = await screen.findByRole('heading', { name: /Station Overview/i });
    // Find table under overview and assert station name inside table (avoid map overlay duplicate)
    const table = overviewHeading.parentElement?.querySelector('table') as HTMLTableElement;
    expect(table).toBeTruthy();
    expect(within(table).getByText('Station #1')).toBeInTheDocument();
  });
});


