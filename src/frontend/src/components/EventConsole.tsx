import { useEffect, useState } from "react";
import { appConfig } from "../config/env";

// 1. Define the props to accept the token
type EventConsoleProps = {
    token: string | null;
};

export default function EventConsole({ token }: EventConsoleProps) {
    const [events, setEvents] = useState<string[]>([]);
    const [connected, setConnected] = useState(false);
    const maxEvents = 300;

    useEffect(() => {
        let es: EventSource | null = null;
        let isMounted = true;

        // 2. Create headers for fetch() requests
        const headers = {
            "Authorization": `Bearer ${token}`,
            "Content-Type": "application/json",
        };

        async function loadInitial() {
            if (!token) return; // <-- USES TOKEN
            try {
                // 3. Use the headers in your fetch
                const res = await fetch(`${appConfig.apiUrl}/events`, { headers });
                if (!res.ok) return;
                const list = await res.json();
                if (!isMounted) return;
                setEvents((prev) => mergeLatest(list, prev));
            } catch {
                // ignore
            }
        }

        if (token) {
            loadInitial();
        }

        try {
            // 4. Don't connect if no token
            if (!token) {
                setConnected(false);
                return;
            }

            // 5. Send the token as a query parameter for EventSource
            es = new EventSource(`${appConfig.apiUrl}/events/stream?token=${token}`);
            setConnected(true);

            const push = (data: string) => {
                setEvents((prev) => {
                    const next = [data, ...prev];
                    return next.slice(0, maxEvents);
                });
            };

            es.onmessage = (ev) => push(ev.data);
            const names = [
                "ReservationCreated",
                "ReservationExpired",
                "TripStarted",
                "TripEnded",
                "TripBilled",
                "BikeStatusChanged",
                "BillIssued",
                "BikeMovedEvent",
                "StationStatusChangedEvent",
                "StationCapacityChangedEvent"
            ];
            for (const n of names) {
                es.addEventListener(n, (ev: MessageEvent) => push(ev.data));
            }

            es.onerror = () => {
                setConnected(false);
            };
        } catch {
            setConnected(false);
        }

        return () => {
            isMounted = false;
            if (es) {
                es.close();
            }
        };
    }, [token]); // 6. Re-run this effect if the token changes

    // 7. Define headers for the Refresh button's fetch
    const refreshHeaders = {
        "Authorization": `Bearer ${token}`,
        "Content-Type": "application/json",
    };

    return (
        <div style={{ padding: 12, border: "1px solid #e5e5e5", borderRadius: 6, background: "#fff", maxHeight: 520, overflow: "auto" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                <strong>Event Console</strong>
                <div style={{ fontSize: 12, color: connected ? "green" : "gray" }}>{connected ? "Live" : "Disconnected"}</div>
            </div>

            <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                <button
                    onClick={() => {
                        if (!token) return;
                        // 8. Use headers in the Refresh button
                        fetch(`${appConfig.apiUrl}/events`, { headers: refreshHeaders })
                            .then((r) => r.json())
                            .then((list) => setEvents(list.reverse ? list.slice().reverse() : list));
                    }}
                    disabled={!token}
                >
                    Refresh
                </button>
                <button onClick={() => setEvents([])}>Clear</button>
            </div>

            <div style={{ fontFamily: "monospace", fontSize: 13, whiteSpace: "pre-wrap" }}>
                {events.length === 0 && <div style={{ color: "#666" }}>No events yet</div>}
                {events.map((e, idx) => (
                    <div key={idx} style={{ padding: "6px 8px", borderBottom: "1px solid #f0f0f0" }}>
                        {e}
                    </div>
                ))}
            </div>
        </div>
    );
}

function mergeLatest(incoming: string[], prev: string[]) {
    // server /events returns newest-first formatted strings (depends on backend). Ensure newest first.
    const items = Array.isArray(incoming) ? incoming.slice(0, 300) : [];
    // Keep unique prefix (basic de-dup)
    const set = new Set(items.concat(prev));
    return Array.from(set).slice(0, 300);
}