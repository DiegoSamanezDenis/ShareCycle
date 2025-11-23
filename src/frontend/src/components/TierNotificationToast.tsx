import { useEffect, useState, useRef } from "react";
import { appConfig } from "../config/env";

export default function TierNotificationToast({ token }: { token: string | null }) {
    const [message, setMessage] = useState<string | null>(null);
    const [visible, setVisible] = useState(false);

    // Track when the component mounted
    const mountTimeRef = useRef(Date.now());

    useEffect(() => {
        if (!token) return;

        const es = new EventSource(`${appConfig.apiUrl}/events/stream?token=${token}`);

        es.addEventListener("TierUpdatedEvent", (event: MessageEvent) => {
            // Parse the ID (timestamp) from the event if possible, or just ignore initial burst
            // The backend sends ID as epoch seconds.
            // If the event ID is older than our mount time, ignore it.

            if (event.lastEventId) {
                const eventTime = parseInt(event.lastEventId) * 1000; // Convert sec to ms
                // Give a 2-second buffer for clock drift/latency
                if (eventTime < mountTimeRef.current - 2000) {
                    return; // Ignore old history events
                }
            }

            const rawText = event.data || "";
            const cleanMessage = rawText.includes(" - ") ? rawText.split(" - ")[1] : rawText;

            setMessage(cleanMessage);
            setVisible(true);
            setTimeout(() => setVisible(false), 6000);
        });

        es.onerror = () => { };

        return () => {
            es.close();
        };
    }, [token]);

    if (!visible || !message) return null;

    return (
        <div style={{
            position: "fixed",
            top: "24px",
            right: "24px",
            background: "linear-gradient(135deg, #F59E0B 0%, #D97706 100%)", // Gold gradient
            color: "white",
            padding: "16px 20px",
            borderRadius: "8px",
            boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)",
            zIndex: 9999,
            maxWidth: "400px",
            animation: "fadeIn 0.3s ease-in-out",
            fontFamily: "system-ui, sans-serif"
        }}>
            <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                <span style={{ fontSize: "24px" }}>🎉</span>
                <div>
                    <strong style={{ display: "block", marginBottom: "4px", fontSize: "16px" }}>
                        Status Updated!
                    </strong>
                    <span style={{ fontSize: "14px", lineHeight: "1.4" }}>
            {message}
          </span>
                </div>
                <button
                    onClick={() => setVisible(false)}
                    style={{
                        background: "none",
                        border: "none",
                        color: "white",
                        opacity: 0.8,
                        fontSize: "20px",
                        cursor: "pointer",
                        marginLeft: "8px",
                        padding: "0 4px"
                    }}
                >
                    &times;
                </button>
            </div>
        </div>
    );
}