import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import ReserveForm from '../../pages/ReserveForm';
import { AuthProvider } from '../../auth/AuthContext';

vi.mock('../../api/client', () => ({
  apiRequest: vi.fn(async (path: string, opts?: any) => {
    // Mock bikes endpoint for station s1
    if (path === '/stations/s1/bikes') {
      return [
        { id: 'b1', type: 'STANDARD', label: 'Bike A' },
        { id: 'b2', type: 'STANDARD', label: 'Bike B' }
      ];
    }
    // Mock reservation POST
    if (path === '/reservations' && opts && opts.method === 'POST') {
      return { id: 'r1', expiresAt: new Date(Date.now() + 5 * 60_000).toISOString() };
    }
    return undefined;
  })
}));

describe('ReserveForm', () => {
  beforeEach(() => {
    localStorage.clear();
    localStorage.setItem(
      'sharecycle.auth',
      JSON.stringify({ token: 't', role: 'RIDER', userId: 'u1', username: 'r1' })
    );
  });

  it('fetches bikes for selected station and allows picking a bike', async () => {
    const stations = [
      { id: 's1', name: 'Station 1' },
      { id: 's2', name: 'Station 2' }
    ];

    render(
      <AuthProvider>
        <ReserveForm stations={stations} />
      </AuthProvider>
    );

    // station select should be present
    const stationSelect = screen.getByTestId('station-select') as HTMLSelectElement;
    expect(stationSelect).toBeInTheDocument();

    // select s1 and wait for bikes to load
    await userEvent.selectOptions(stationSelect, 's1');

    // bike select should appear and contain options
    const bikeSelect = await screen.findByTestId('bike-select');
    await waitFor(() => {
      expect(bikeSelect).toBeInTheDocument();
    });

    // options should include Bike A and Bike B
    expect(screen.getByRole('option', { name: /Bike A/ })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: /Bike B/ })).toBeInTheDocument();

    // choose Bike B and submit
    await userEvent.selectOptions(bikeSelect as HTMLSelectElement, 'b2');
    await userEvent.click(screen.getByRole('button', { name: /Reserve bike/i }));

    // reservation feedback shown
    expect(await screen.findByText(/Reservation created:/)).toBeInTheDocument();
  });
});

