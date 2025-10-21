import React, {useEffect, useState} from 'react';
import {apiRequest} from '../api/client';
import {useAuth} from '../auth/AuthContext';

type Station = { id: string; name: string};
type Bike = { id: string; type?: string; label?: string };

export default function ReserveForm({stations = []}: {stations?: Station[]}) {
    const {userId, token} = useAuth();
    const [stationId, setStationId] = useState<string>(() => stations?.[0]?.id ?? '');
    const [bikeId, setBikeId] = useState<string>('');
    const [expiresAfterMinutes, setExpiresAfterMinutes] = useState<number>(15);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [reservation, setReservation] = useState<{ id: string; expiresAt: string } | null>(null);
    const [remaining, setRemaining] = useState<number | null>(null);

    // Bikes for the selected station
    const [bikes, setBikes] = useState<Bike[]>([]);
    const [bikesLoading, setBikesLoading] = useState(false);
    const [bikesError, setBikesError] = useState<string | null>(null);

    useEffect(() => {
        if (!stations || stations.length === 0) return;
        if (!stationId) setStationId(stations[0].id);
        // keep stationId in sync when stations prop first arrives
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [stations]);

    // Fetch bikes when station changes
    useEffect(() => {
        async function loadBikesForStation(id: string) {
            if (!id) {
                setBikes([]);
                setBikesError(null);
                setBikesLoading(false);
                return;
            }
            setBikesLoading(true);
            setBikesError(null);
            try {
                // NOTE: backend doesn't currently expose this endpoint in some setups; tests can mock it.
                // don't pass token here — listing available bikes is a public/activity endpoint
                const data = await apiRequest<Bike[]>(`/stations/${id}/bikes`);
                // Normalize to expected shape (avoid `any` to satisfy ESLint)
                const normalized = (data ?? []).map((b: unknown) => {
                    const obj = (b ?? {}) as Record<string, unknown>;
                    const rawId = obj.id ?? obj.bikeId ?? obj;
                    const idStr = typeof rawId === 'string' ? rawId : String(rawId ?? '');
                    const type = typeof obj.type === 'string' ? obj.type : undefined;
                    const label = typeof obj.label === 'string' ? obj.label : idStr;
                    return { id: idStr, type, label } as Bike;
                });
                setBikes(normalized);
                // Reset selected bike when station changes
                setBikeId('');
            } catch (err: unknown) {
                // If endpoint not available or returns 404, fall back to empty list (user can pick any bike)
                setBikes([]);
                const message = err instanceof Error ? err.message : String(err ?? 'Unable to load bikes');
                setBikesError(message);
            } finally {
                setBikesLoading(false);
            }
        }

        void loadBikesForStation(stationId);
    }, [stationId, token]);

    useEffect(() => {
        if (!reservation) return;
        const update = () => {
            const expires = new Date(reservation.expiresAt).getTime();
            const now = Date.now();
            const ms = Math.max(0, expires - now);
            setRemaining(ms);
        };
        update();
        const id = setInterval(update, 1000);
        return () => clearInterval(id);
    }, [reservation]);

    const fmt = (ms: number) => {
        const total = Math.max(0, Math.floor(ms / 1000));
        const mins = Math.floor(total / 60)
            .toString()
            .padStart(2, '0');
        const secs = (total % 60).toString().padStart(2, '0');
        return `${mins}:${secs}`;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            if (!userId) {
                setError('No rider logged in');
                setLoading(false);
                return;
            }
            const payload = {
                riderId: userId,
                stationId,
                bikeId: bikeId || undefined,
                expiresAfterMinutes
            };

            // Debug: print token and payload just before request
            // eslint-disable-next-line no-console
            console.debug('[ReserveForm] submitting reservation', { token, payload });

            // Do not send Authorization header for this POST; backend uses riderId in body
            const resp = await apiRequest<{ id: string; expiresAt?: string }>('/reservations', {
                method: 'POST',
                body: JSON.stringify(payload)
            });

            const expiresAt = resp.expiresAt ?? new Date(Date.now() + expiresAfterMinutes * 60_000).toISOString();
            setReservation({ id: resp.id, expiresAt });
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : String(err ?? 'Failed to create reservation');
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <section aria-label="reserve-form" style={{ marginTop: 24 }}>
            <h3>Reserve a bike</h3>
            <form onSubmit={handleSubmit}>
                <label>
                    Station
                    <select value={stationId} onChange={(e) => setStationId(e.target.value)} data-testid="station-select">
                        {stations.map((s) => (
                            <option key={s.id} value={s.id}>
                                {s.name}
                            </option>
                        ))}
                    </select>
                </label>

                {/* Bike selector - appears after station selected */}
                <label>
                    Bike (optional)
                    {bikesLoading ? (
                        <div>Loading bikes…</div>
                    ) : bikes.length > 0 ? (
                        <select value={bikeId} onChange={(e) => setBikeId(e.target.value)} data-testid="bike-select">
                            <option value="">Any available bike</option>
                            {bikes.map((b) => (
                                <option key={b.id} value={b.id}>{b.label ?? b.id}</option>
                            ))}
                        </select>
                    ) : (
                        <select value={bikeId} onChange={(e) => setBikeId(e.target.value)} data-testid="bike-select">
                            <option value="">Any available bike</option>
                        </select>
                    )}
                    {bikesError && <div style={{ color: 'orange' }}>{bikesError}</div>}
                </label>

                <label>
                    Expires (minutes)
                    <input
                        type="number"
                        min={1}
                        value={expiresAfterMinutes}
                        onChange={(e) => setExpiresAfterMinutes(Number(e.target.value))}
                    />
                </label>

                <button type="submit" disabled={loading}>
                    {loading ? 'Reserving…' : 'Reserve bike'}
                </button>

                {error && (
                    <div role="alert" style={{ color: 'red', marginTop: 8 }}>
                        {error}
                    </div>
                )}
            </form>

            {reservation && (
                <div role="status" style={{ marginTop: 12 }}>
                    <div>Reservation created: {reservation.id}</div>
                    <div>
                        Expires in: {remaining !== null ? fmt(remaining) : 'calculating'}
                        {remaining === 0 && <span> — expired</span>}
                    </div>
                </div>
            )}
        </section>
    );
}