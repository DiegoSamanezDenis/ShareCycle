type Dock = { index: number; status: "empty" | "occupied" | "out_of_service"; bikeId?: string | null };

export default function DockGrid({ docks }: { docks: Dock[] }) {
    // basic responsive grid. You can tune columns depending on docks.length.
    const columns = Math.min(6, Math.max(3, Math.ceil(Math.sqrt(docks.length))));
    return (
        <div>
            <div style={{ display: "grid", gridTemplateColumns: `repeat(${columns}, 1fr)`, gap: 8 }}>
                {docks.map((d) => (
                    <div
                        key={d.index}
                        style={{
                            border: "1px solid #ccc",
                            borderRadius: 6,
                            padding: 8,
                            textAlign: "center",
                            background: d.status === "occupied" ? "#e6f7ff" : d.status === "out_of_service" ? "#fff1f0" : "#f6ffed",
                        }}
                    >
                        <div style={{ fontSize: 12, color: "#666" }}>Dock {d.index}</div>
                        <div style={{ fontWeight: 600, marginTop: 6 }}>
                            {d.status === "occupied" ? "Occupied" : d.status === "empty" ? "Empty" : "Out of service"}
                        </div>
                        <div style={{ marginTop: 8, fontSize: 12 }}>{d.bikeId ? `Bike: ${d.bikeId}` : ""}</div>
                    </div>
                ))}
            </div>

            <div style={{ marginTop: 8, display: "flex", gap: 12, fontSize: 13 }}>
                <Legend color="#e6f7ff" label="Occupied" />
                <Legend color="#f6ffed" label="Empty" />
                <Legend color="#fff1f0" label="Out of service" />
            </div>
        </div>
    );
}

function Legend({ color, label }: { color: string; label: string }) {
    return (
        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            <div style={{ width: 14, height: 14, background: color, border: "1px solid #ccc", borderRadius: 3 }} />
            <div style={{ color: "#444" }}>{label}</div>
        </div>
    );
}
