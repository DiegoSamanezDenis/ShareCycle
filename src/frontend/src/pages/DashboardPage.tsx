import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
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

type ReservationResponse = {
  reservationId: string;
  stationId: string;
  bikeId: string;
  reservedAt: string;
  expiresAt: string;
  active: boolean;
};

type TripResponse = {
  tripId: string;
  stationId: string;
  bikeId: string;
  riderId: string;
  startedAt: string;
};

type TripCompletionResponse = {
  tripId: string;
  endStationId: string;
  endedAt: string;
  durationMinutes: number;
  ledgerId: string;
  totalAmount: number;
};

type ReservationFormState = {
  stationId: string;
  bikeId: string;
  expiresAfterMinutes: string;
};

type StartTripFormState = {
  tripId: string;
  bikeId: string;
  stationId: string;
};

type EndTripFormState = {
  tripId: string;
  stationId: string;
};

type StatusFormState = {
  stationId: string;
  outOfService: boolean;
};

type CapacityFormState = {
  stationId: string;
  delta: string;
};

type MoveBikeFormState = {
  bikeId: string;
  destinationStationId: string;
};

const defaultReservationForm: ReservationFormState = {
  stationId: '',
  bikeId: '',
  expiresAfterMinutes: '5'
};

const defaultStartTripForm: StartTripFormState = {
  tripId: '',
  bikeId: '',
  stationId: ''
};

const defaultEndTripForm: EndTripFormState = {
  tripId: '',
  stationId: ''
};

const defaultStatusForm: StatusFormState = {
  stationId: '',
  outOfService: false
};

const defaultCapacityForm: CapacityFormState = {
  stationId: '',
  delta: '0'
};

const defaultMoveBikeForm: MoveBikeFormState = {
  bikeId: '',
  destinationStationId: ''
};

export default function DashboardPage() {
  const auth = useAuth();

  const [stations, setStations] = useState<StationSummary[]>([]);
  const [loadingStations, setLoadingStations] = useState(false);
  const [stationsError, setStationsError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [reservationForm, setReservationForm] = useState(defaultReservationForm);
  const [reservationResult, setReservationResult] = useState<ReservationResponse | null>(null);

  const [startTripForm, setStartTripForm] = useState(defaultStartTripForm);
  const [tripResult, setTripResult] = useState<TripResponse | null>(null);

  const [endTripForm, setEndTripForm] = useState(defaultEndTripForm);
  const [tripCompletion, setTripCompletion] = useState<TripCompletionResponse | null>(null);

  const [statusForm, setStatusForm] = useState(defaultStatusForm);
  const [capacityForm, setCapacityForm] = useState(defaultCapacityForm);
  const [moveBikeForm, setMoveBikeForm] = useState(defaultMoveBikeForm);

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

  if (!auth.token || !auth.role || !auth.userId) {
    return (
      <main>
        <h1>Dashboard</h1>
        <p>You need to sign in to access ShareCycle operations.</p>
      </main>
    );
  }

  const handleReserve = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    setReservationResult(null);
    try {
      const response = await apiRequest<ReservationResponse>('/reservations', {
        method: 'POST',
        token: auth.token,
        body: JSON.stringify({
          riderId: auth.userId,
          stationId: reservationForm.stationId,
          bikeId: reservationForm.bikeId,
          expiresAfterMinutes: Number(reservationForm.expiresAfterMinutes) || 5
        })
      });
      setReservationResult(response);
      setFeedback('Reservation created successfully.');
      setReservationForm(defaultReservationForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Reservation failed');
    }
  };

  const handleStartTrip = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    setTripResult(null);
    try {
      const response = await apiRequest<TripResponse>('/trips', {
        method: 'POST',
        token: auth.token,
        body: JSON.stringify({
          tripId: startTripForm.tripId || null,
          riderId: auth.userId,
          bikeId: startTripForm.bikeId,
          stationId: startTripForm.stationId,
          startTime: null
        })
      });
      setTripResult(response);
      setFeedback('Trip started.');
      setStartTripForm(defaultStartTripForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Unable to start trip');
    }
  };

  const handleEndTrip = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    setTripCompletion(null);
    if (!endTripForm.tripId) {
      setFeedback('Trip ID is required to end a trip.');
      return;
    }
    try {
      const response = await apiRequest<TripCompletionResponse>(`/trips/${endTripForm.tripId}/end`, {
        method: 'POST',
        token: auth.token,
        body: JSON.stringify({
          stationId: endTripForm.stationId
        })
      });
      setTripCompletion(response);
      setFeedback('Trip completed.');
      setEndTripForm(defaultEndTripForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Unable to end trip');
    }
  };

  const handleStatusUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    try {
      await apiRequest<StationSummary>(
        `/stations/${statusForm.stationId}/status`,
        {
          method: 'PATCH',
          token: auth.token,
          body: JSON.stringify({
            operatorId: auth.userId,
            outOfService: statusForm.outOfService
          })
        }
      );
      setFeedback('Station status updated.');
      setStatusForm(defaultStatusForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Unable to update station status');
    }
  };

  const handleCapacityUpdate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    try {
      await apiRequest<StationSummary>(
        `/stations/${capacityForm.stationId}/capacity`,
        {
          method: 'PATCH',
          token: auth.token,
          body: JSON.stringify({
            operatorId: auth.userId,
            delta: Number(capacityForm.delta) || 0
          })
        }
      );
      setFeedback('Station capacity updated.');
      setCapacityForm(defaultCapacityForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Unable to update capacity');
    }
  };

  const handleMoveBike = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    try {
      await apiRequest<StationSummary[]>('/stations/move-bike', {
        method: 'POST',
        token: auth.token,
        body: JSON.stringify({
          operatorId: auth.userId,
          bikeId: moveBikeForm.bikeId,
          destinationStationId: moveBikeForm.destinationStationId
        })
      });
      setFeedback('Bike moved successfully.');
      setMoveBikeForm(defaultMoveBikeForm);
      await loadStations();
    } catch (err) {
      setFeedback(err instanceof Error ? err.message : 'Unable to move bike');
    }
  };

  return (
    <main>
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

      {auth.role === 'RIDER' && (
        <section>
          <h2>Rider Actions</h2>

          <form onSubmit={handleReserve}>
            <h3>Reserve a bike</h3>
            <label>
              Station ID
              <input
                required
                value={reservationForm.stationId}
                onChange={(event) =>
                  setReservationForm((current) => ({ ...current, stationId: event.target.value }))
                }
              />
            </label>
            <label>
              Bike ID
              <input
                required
                value={reservationForm.bikeId}
                onChange={(event) =>
                  setReservationForm((current) => ({ ...current, bikeId: event.target.value }))
                }
              />
            </label>
            <label>
              Hold time (minutes)
              <input
                required
                type="number"
                min="1"
                value={reservationForm.expiresAfterMinutes}
                onChange={(event) =>
                  setReservationForm((current) => ({
                    ...current,
                    expiresAfterMinutes: event.target.value
                  }))
                }
              />
            </label>
            <button type="submit">Reserve bike</button>
          </form>

          <form onSubmit={handleStartTrip}>
            <h3>Start a trip</h3>
            <label>
              Optional Trip ID
              <input
                value={startTripForm.tripId}
                onChange={(event) =>
                  setStartTripForm((current) => ({ ...current, tripId: event.target.value }))
                }
                placeholder="Leave blank to auto-generate"
              />
            </label>
            <label>
              Bike ID
              <input
                required
                value={startTripForm.bikeId}
                onChange={(event) =>
                  setStartTripForm((current) => ({ ...current, bikeId: event.target.value }))
                }
              />
            </label>
            <label>
              Station ID
              <input
                required
                value={startTripForm.stationId}
                onChange={(event) =>
                  setStartTripForm((current) => ({ ...current, stationId: event.target.value }))
                }
              />
            </label>
            <button type="submit">Start trip</button>
          </form>

          <form onSubmit={handleEndTrip}>
            <h3>End a trip</h3>
            <label>
              Trip ID
              <input
                required
                value={endTripForm.tripId}
                onChange={(event) =>
                  setEndTripForm((current) => ({ ...current, tripId: event.target.value }))
                }
              />
            </label>
            <label>
              Destination Station ID
              <input
                required
                value={endTripForm.stationId}
                onChange={(event) =>
                  setEndTripForm((current) => ({ ...current, stationId: event.target.value }))
                }
              />
            </label>
            <button type="submit">Complete trip</button>
          </form>
        </section>
      )}

      {auth.role === 'OPERATOR' && (
        <section>
          <h2>Operator Controls</h2>

          <form onSubmit={handleStatusUpdate}>
            <h3>Toggle station status</h3>
            <label>
              Station ID
              <input
                required
                value={statusForm.stationId}
                onChange={(event) =>
                  setStatusForm((current) => ({ ...current, stationId: event.target.value }))
                }
              />
            </label>
            <label>
              Out of service?
              <input
                type="checkbox"
                checked={statusForm.outOfService}
                onChange={(event) =>
                  setStatusForm((current) => ({ ...current, outOfService: event.target.checked }))
                }
              />
            </label>
            <button type="submit">Update status</button>
          </form>

          <form onSubmit={handleCapacityUpdate}>
            <h3>Adjust capacity</h3>
            <label>
              Station ID
              <input
                required
                value={capacityForm.stationId}
                onChange={(event) =>
                  setCapacityForm((current) => ({ ...current, stationId: event.target.value }))
                }
              />
            </label>
            <label>
              Delta (positive or negative)
              <input
                required
                type="number"
                value={capacityForm.delta}
                onChange={(event) =>
                  setCapacityForm((current) => ({ ...current, delta: event.target.value }))
                }
              />
            </label>
            <button type="submit">Apply change</button>
          </form>

          <form onSubmit={handleMoveBike}>
            <h3>Move a bike</h3>
            <label>
              Bike ID
              <input
                required
                value={moveBikeForm.bikeId}
                onChange={(event) =>
                  setMoveBikeForm((current) => ({ ...current, bikeId: event.target.value }))
                }
              />
            </label>
            <label>
              Destination Station ID
              <input
                required
                value={moveBikeForm.destinationStationId}
                onChange={(event) =>
                  setMoveBikeForm((current) => ({
                    ...current,
                    destinationStationId: event.target.value
                  }))
                }
              />
            </label>
            <button type="submit">Move bike</button>
          </form>
        </section>
      )}

      <section>
        <h2>Activity feedback</h2>
        {feedback && <p>{feedback}</p>}
        {reservationResult && (
          <p>
            Reservation {reservationResult.reservationId} valid until{' '}
            {new Date(reservationResult.expiresAt).toLocaleString()}.
          </p>
        )}
        {tripResult && <p>Trip {tripResult.tripId} started at {new Date(tripResult.startedAt).toLocaleString()}.</p>}
        {tripCompletion && (
          <p>
            Trip {tripCompletion.tripId} ended at {new Date(tripCompletion.endedAt).toLocaleString()}.
            Charge: ${tripCompletion.totalAmount.toFixed(2)}.
          </p>
        )}
      </section>
    </main>
  );
}
