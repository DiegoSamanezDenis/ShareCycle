import { useEffect, useState } from "react";
import DockGrid from "./DockGrid";

type Dock = { index: number; status: "empty" | "occupied" | "out_of_service"; bikeId?: string | null };
type StationDetails = {
    id: string;
    name: string;
    docks: Dock[];
    // server-supplied permission/state flags
    canReserve?: boolean;
    canStartTrip?: boolean;
    canReturn?: boolean;
    canMove?: boolean;
    canToggleStatus?: boolean;
    role?: "rider" | "operator" | string;
};

type Props = {
    stationId: string;
    onClose?: () => void;
};

function getErrorMessage(err: unknown): string {
    if (err instanceof Error) return err.message;
    try {
        // attempt to stringify non-Error values
        return String(err ?? "Unknown error");
    } catch {
        return "Unknown error";
    }
}

export default function StationDetailsDrawer({ stationId, onClose }: Props) {
    const [details, setDetails] = useState<StationDetails | null>(null);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [message, setMessage] = useState<string | null>(null);

    async function fetchDetails() {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`/api/stations/${stationId}/details`);
            if (!res.ok) throw new Error(`fetch failed ${res.status}`);
            const json = await res.json();
            setDetails(json);
        } catch (ex: unknown) {
            setError(getErrorMessage(ex));
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchDetails();
        // also refresh after a short interval to reflect actions (optional)
        const interval = setInterval(fetchDetails, 30_000);
        return () => clearInterval(interval);
    }, [stationId]);

    async function runAction(path: string, body?: Record<string, unknown>) {
        setActionLoading(true);
        setError(null);
        setMessage(null);
        try {
            const res = await fetch(`/api/stations/${stationId}/${path}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: body ? JSON.stringify(body) : undefined,
            });
            if (!res.ok) {
                const txt = await res.text();
                throw new Error(txt || `action failed ${res.status}`);
            }
            setMessage("Action completed");
            // read model will reflect the action; refresh
            await fetchDetails();
        } catch (ex: unknown) {
            setError(getErrorMessage(ex));
        } finally {
            setActionLoading(false);
        }
    }

    if (!details && loading) return <div className="drawer">Loading station...</div>;
    if (!details) return <div className="drawer">No details</div>;

    return (
        <div className="drawer" style={{ width: 420, padding: 16, borderLeft: "1px solid #ddd", background: "#fff" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h3 style={{ margin: 0 }}>{details.name}</h3>
                <div>
                    <button onClick={() => { fetchDetails(); }} disabled={loading} style={{ marginRight: 8 }}>
                        Refresh
                    </button>
                    <button onClick={onClose}>Close</button>
                </div>
            </div>

            <div style={{ marginTop: 12 }}>
                <strong>Role:</strong> {details.role ?? "unknown"}
            </div>

            <div style={{ marginTop: 12 }}>
                <DockGrid docks={details.docks} />
            </div>

            <div style={{ marginTop: 14, display: "flex", flexDirection: "column", gap: 8 }}>
                <div>
                    <button
                        onClick={() => runAction("reserve")}
                        disabled={!details.canReserve || actionLoading}
                        style={{ marginRight: 8 }}
                        title={details.canReserve ? "Reserve a dock" : "Cannot reserve right now"}
                    >
                        Reserve
                    </button>

                    <button
                        onClick={() => runAction("start-trip")}
                        disabled={!details.canStartTrip || actionLoading}
                        style={{ marginRight: 8 }}
                        title={details.canStartTrip ? "Start a trip" : "Cannot start trip"}
                    >
                        Start Trip
                    </button>

                    <button
                        onClick={() => runAction("return")}
                        disabled={!details.canReturn || actionLoading}
                        style={{ marginRight: 8 }}
                        title={details.canReturn ? "Return bike" : "Cannot return"}
                    >
                        Return
                    </button>
                </div>

                <div>
                    <button
                        onClick={() => {
                            const target = window.prompt("Target station id for move (UUID):");
                            if (target) runAction("move", { targetStationId: target });
                        }}
                        disabled={!details.canMove || actionLoading}
                        style={{ marginRight: 8 }}
                        title={details.canMove ? "Move bike to another station" : "Move not allowed"}
                    >
                        Move
                    </button>

                    <button
                        onClick={() => runAction("toggle-status")}
                        disabled={!details.canToggleStatus || actionLoading}
                        title={details.canToggleStatus ? "Toggle station in/out of service" : "Not allowed"}
                    >
                        Toggle Station Status
                    </button>
                </div>
            </div>

            <div style={{ marginTop: 12, minHeight: 24 }}>
                {actionLoading && <span>Working...</span>}
                {message && <div style={{ color: "green" }}>{message}</div>}
                {error && <div style={{ color: "crimson" }}>{error}</div>}
            </div>
        </div>
    );
}
