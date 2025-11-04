import { useEffect, useState } from "react";

export default function EventConsole() {
    const [events, setEvents] = useState<string[]>([]);
    const [connected, setConnected] = useState(false);
    const maxEvents = 300;

    useEffect(() => {
        let es: EventSource | null = null;
        let isMounted = true;

        async function loadInitial() {
            try {
                const res = await fetch("/events");
                if (!res.ok) return;
                const list = await res.json();
                if (!isMounted) return;
                setEvents((prev) => mergeLatest(list, prev));
            } catch {
                // ignore
            }
        }

        loadInitial();

        try {
            es = new EventSource("/events/stream");
            setConnected(true);

            const push = (data: string) => {
                setEvents((prev) => {
                    const next = [data, ...prev];
                    return next.slice(0, maxEvents);
                });
            };

            // generic message fallback
            es.onmessage = (ev) => push(ev.data);

            // register handlers for the known domain event names so named events are also caught
            const names = [
                "ReservationCreated",
                "ReservationExpired",
                "TripStarted",
                "TripEnded",
                "TripBilled",
                "BikeStatusChanged",
                "BillIssued",
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
                try {
                    es.close();
                } catch (err){
                    void err
                }
            }
        };
    }, []);

    return (
        <div style={{ padding: 12, border: "1px solid #e5e5e5", borderRadius: 6, background: "#fff", maxHeight: 520, overflow: "auto" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                <strong>Event Console</strong>
                <div style={{ fontSize: 12, color: connected ? "green" : "gray" }}>{connected ? "Live" : "Disconnected"}</div>
            </div>

            <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
                <button onClick={() => { fetch("/events").then(r => r.json()).then((list) => setEvents(list.reverse ? list.slice().reverse() : list)); }}>
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
