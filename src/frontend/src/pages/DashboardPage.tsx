import { useEffect, useState } from 'react';
// no form submissions in Stories 1–3 UI
import { apiRequest } from '../api/client';
import { useAuth } from '../auth/AuthContext';

type StationSummary = {
  stationId: string;
  name: string | null;
  status: 'EMPTY' | 'OCCUPIED' | 'FULL' | 'OUT_OF_SERVICE';
  bikesDocked: number;
  capacity: number;
  freeDocks: number;
};

// Reservation and operator controls removed for Stories 1–3 only

export default function DashboardPage() {
  const auth = useAuth();

  const [stations, setStations] = useState<StationSummary[]>([]);
  const [loadingStations, setLoadingStations] = useState(false);
  const [stationsError, setStationsError] = useState<string | null>(null);
  // No activity feedback in simplified UI

  useEffect(() => {
    if (auth.token) {
      void loadStations();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [auth.token]);

  const loadStations = async () => {
    setLoadingStations(true);
    setStationsError(null);
    try {
      const data = await apiRequest<StationSummary[]>('/stations', {
        token: auth.token ?? undefined
      });
      setStations(data);
    } catch (err) {
      setStationsError(err instanceof Error ? err.message : 'Unable to load stations');
    } finally {
      setLoadingStations(false);
    }
  };

  // Map and reservation countdown removed for Stories 1–3

  if (!auth.token || !auth.role || !auth.userId) {
    return (
      <main>
        <h1>Dashboard</h1>
        <p>You need to sign in to access ShareCycle operations.</p>
      </main>
    );
  }

  // Reservation and operator handlers removed

  // Capacity/move bike/trip actions removed for Stories 4–9

  return (
    <main>
      {/* Stories 1–3: summaries only */}
      <header>
        <h1>ShareCycle Dashboard</h1>
        <p>
          Signed in as <strong>{auth.username}</strong> ({auth.role.toLowerCase()}).
        </p>
        <button type="button" onClick={() => auth.logout()}>
          Logout
        </button>
      </header>

      <section>
        <h2>Station Overview</h2>
        {loadingStations && <p>Loading stations…</p>}
        {stationsError && <p role="alert">{stationsError}</p>}
        {!loadingStations && !stationsError && (
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th>Bikes docked</th>
                <th>Free docks</th>
                <th>Capacity</th>
              </tr>
            </thead>
            <tbody>
              {stations.map((station) => (
                <tr key={station.stationId}>
                  <td>{station.name ?? 'Unnamed station'}</td>
                  <td>{station.status}</td>
                  <td>{station.bikesDocked}</td>
                  <td>{station.freeDocks}</td>
                  <td>{station.capacity}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {/* No actions or event console for Stories 1–3 */}
    </main>
  );
}
