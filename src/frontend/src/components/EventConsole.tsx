import { useEffect, useState } from "react";
import type { CSSProperties } from "react";
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
            if (!token) return;
            try {
                // 3. Use the headers in your fetch
                const res = await fetch(`${appConfig.apiUrl}/events`, { headers });
                if (!res.ok) return;

                const list = await res.json();
                if (!isMounted) return;

                // Helper handles validation now
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
                    // Ensure prev is always an array
                    const safePrev = Array.isArray(prev) ? prev : [];
                    const next = [data, ...safePrev];
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
                "PaymentStartedEvent",
                "PaymentSucceedEvent",
                "PaymentFailedEvent",
                "BikeStatusChanged",
                "BillIssuedEvent",
                "BikeMovedEvent",
                "StationStatusChangedEvent",
                "StationCapacityChangedEvent",
                "FlexCreditAddedEvent",
                "FlexCreditDeductEvent",
                "TierUpdatedEvent"
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

    const containerStyle: CSSProperties = {
        padding: 12,
        border: "1px solid #d4d4d8",
        borderRadius: 8,
        background: "linear-gradient(180deg, #0f172a 0%, #1e293b 60%, #111827 100%)",
        color: "#e2e8f0",
        maxHeight: 520,
        overflow: "auto",
        boxShadow: "0 6px 18px rgba(15, 23, 42, 0.3)",
    };

    const toolbarStyle: CSSProperties = {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginBottom: 12,
    };

    const headerStyle: CSSProperties = {
        fontWeight: 600,
        letterSpacing: "0.04em",
        textTransform: "uppercase",
        color: "#f8fafc",
    };

    const statusStyle: CSSProperties = {
        fontSize: 12,
        color: connected ? "#22c55e" : "#94a3b8",
    };

    const buttonBarStyle: CSSProperties = {
        display: "flex",
        gap: 8,
        marginBottom: 12,
    };

    const buttonStyle: CSSProperties = {
        padding: "6px 10px",
        borderRadius: 6,
        border: "1px solid #334155",
        background: "#1e293b",
        color: "#e2e8f0",
        fontSize: 12,
        cursor: token ? "pointer" : "not-allowed",
        transition: "transform 0.1s ease, background 0.2s ease",
    };

    const disabledButtonStyle: CSSProperties = {
        ...buttonStyle,
        opacity: 0.4,
        cursor: "not-allowed",
    };

    const clearButtonStyle: CSSProperties = {
        ...buttonStyle,
        cursor: "pointer",
    };

    const eventsWrapperStyle: CSSProperties = {
        fontFamily: "ui-monospace, SFMono-Regular, SFMono, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace",
        fontSize: 13,
        whiteSpace: "pre-wrap",
        background: "rgba(15, 23, 42, 0.6)",
        borderRadius: 6,
        border: "1px solid rgba(148, 163, 184, 0.2)",
    };

    const eventRowStyle: CSSProperties = {
        padding: "6px 10px",
        borderBottom: "1px solid rgba(148, 163, 184, 0.15)",
    };

    return (
        <div style={containerStyle}>
            <div style={toolbarStyle}>
                <strong style={headerStyle}>Event Console</strong>
                <div style={statusStyle}>{connected ? "Live" : "Disconnected"}</div>
            </div>

            <div style={buttonBarStyle}>
                <button
                    style={token ? buttonStyle : disabledButtonStyle}
                    onClick={async () => {
                        if (!token) return;
                        try {
                            // 8. Use headers in the Refresh button
                            const r = await fetch(`${appConfig.apiUrl}/events`, { headers: refreshHeaders });
                            if (!r.ok) return;
                            const list = await r.json();

                            // Safety Check: Ensure list is actually an array before setting state
                            if (Array.isArray(list)) {
                                setEvents(list.reverse() ? list.slice().reverse() : list);
                            }
                        } catch (e) {
                            console.error("Failed to refresh events", e);
                        }
                    }}
                    disabled={!token}
                >
                    Refresh
                </button>
                <button
                    style={clearButtonStyle}
                    onClick={() => setEvents([])}
                >
                    Clear
                </button>
            </div>

            <div style={eventsWrapperStyle}>
                {/* Add Array.isArray checks to prevent render crashes */}
                {Array.isArray(events) && events.length === 0 && <div style={{ ...eventRowStyle, color: "#bae6fd" }}>No events yet</div>}

                {Array.isArray(events) && events.map((e, idx) => (
                    <div key={idx} style={{ ...eventRowStyle, color: "#f8fafc" }}>
                        {e}
                    </div>
                ))}
            </div>
        </div>
    );
}

function mergeLatest(incoming: string[], prev: string[]) {
    // Safety Check: ensure inputs are arrays
    const safeIncoming = Array.isArray(incoming) ? incoming : [];
    const safePrev = Array.isArray(prev) ? prev : [];

    // server /events returns newest-first formatted strings.
    const items = safeIncoming.slice(0, 300);
    // Keep unique prefix (basic de-dup)
    const set = new Set(items.concat(safePrev));
    return Array.from(set).slice(0, 300);
}