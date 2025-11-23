import { Map, Marker as PigeonMarker } from "pigeon-maps";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../api/client";
import type { FullnessCategory, StationSummary } from "../types/station";
import AppShell from "../components/layout/AppShell";
import PageSection from "../components/layout/PageSection";

export default function HomePage() {
  const [stations, setStations] = useState<StationSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadStations();
  }, []);

  const fullnessColors = useMemo(
    () =>
      ({
        EMPTY: "#1d4ed8",
        LOW: "#0ea5e9",
        HEALTHY: "#22c55e",
        FULL: "#ef4444",
        UNKNOWN: "#6b7280",
      }) satisfies Record<FullnessCategory, string>,
    [],
  );

  const fullnessLegend = useMemo(
    () =>
      [
        ["EMPTY", "No bikes"],
        ["LOW", "Low supply"],
        ["HEALTHY", "Balanced"],
        ["FULL", "No docks"],
      ] as Array<[FullnessCategory, string]>,
    [],
  );

  async function loadStations() {
    setLoading(true);
    setError(null);
    try {
      const data = await apiRequest<StationSummary[]>("/public/stations", {
        skipAuth: true,
      });
      setStations(data);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load stations right now.",
      );
    } finally {
      setLoading(false);
    }
  }

  const heroActions = (
    <>
      <Link
        to="/register"
        style={{
          borderRadius: 999,
          padding: "0.65rem 1.4rem",
          background: "var(--brand)",
          color: "#fff",
          fontWeight: 600,
        }}
      >
        Create rider account
      </Link>
      <Link
        to="/pricing"
        style={{
          borderRadius: 999,
          padding: "0.65rem 1.4rem",
          border: "1px solid var(--border)",
          fontWeight: 600,
          color: "var(--text)",
        }}
      >
        View pricing plans
      </Link>
    </>
  );

  const highlights = [
    {
      title: "Reserve remotely",
      body: "Hold a bike for five minutes while you walk to the station.",
    },
    {
      title: "Ride as rider or operator",
      body: "Toggle roles to monitor the fleet, move bikes, and reset demos.",
    },
    {
      title: "Track loyalty rewards",
      body: "Earn tiered discounts and flex credit when you return bikes on time.",
    },
  ];

  return (
    <AppShell
      heading="City rides made simple"
      subheading="Browse stations, reserve a bike, and switch between rider and operator tools without leaving your browser."
      actions={heroActions}
    >
      <PageSection>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: "1rem",
          }}
        >
          {highlights.map((item) => (
            <div
              key={item.title}
              style={{
                border: "1px solid var(--border)",
                borderRadius: 16,
                padding: "1rem",
                background: "var(--surface-muted)",
              }}
            >
              <h3 style={{ marginBottom: "0.35rem", fontSize: "1.05rem" }}>
                {item.title}
              </h3>
              <p style={{ margin: 0 }}>{item.body}</p>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection
        title="Explore stations"
        description="Guests can browse availability before signing in. Tap any marker to preview status."
      >
        {loading && <p>Loading stations…</p>}
        {error && <p role="alert">{error}</p>}

        {!loading && !error && stations.length === 0 && (
          <p>No stations available yet. Check back soon!</p>
        )}

        {!loading && !error && stations.length > 0 && (
          <>
            <div
              style={{
                display: "flex",
                gap: 16,
                alignItems: "center",
                flexWrap: "wrap",
                margin: "0 0 16px",
                fontSize: 15,
              }}
            >
              <strong>Legend:</strong>
              <div
                style={{
                  display: "flex",
                  gap: 16,
                  alignItems: "center",
                  flexWrap: "wrap",
                }}
              >
                {fullnessLegend.map(([category, label]) => (
                  <span
                    key={category}
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 6,
                    }}
                  >
                    <span
                      style={{
                        width: 18,
                        height: 18,
                        borderRadius: 3,
                        background: fullnessColors[category],
                      }}
                    ></span>
                    {label}
                  </span>
                ))}
              </div>
            </div>
            <div
              style={{
                borderRadius: 16,
                overflow: "hidden",
                border: "1px solid var(--border)",
              }}
            >
              <Map
                defaultCenter={[45.508, -73.587]}
                defaultZoom={13}
                height={400}
                provider={(x: number, y: number, z: number) =>
                  `https://a.tile.openstreetmap.org/${z}/${x}/${y}.png`
                }
              >
                {stations.map((station) => {
                  const lat = Number.isFinite(station.latitude)
                    ? station.latitude
                    : 45.508;
                  const lng = Number.isFinite(station.longitude)
                    ? station.longitude
                    : -73.587;
                  const fullness = (station.fullnessCategory ??
                    "UNKNOWN") as FullnessCategory;
                  const label = station.name ?? "Station";
                  return (
                    <PigeonMarker
                      key={station.stationId}
                      width={64}
                      anchor={[lat, lng]}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 12,
                        }}
                      >
                        <span
                          aria-label={`Station fullness ${fullness.toLowerCase()}`}
                          title={`${label} - ${fullness.toLowerCase()}`}
                          style={{
                            width: 32,
                            height: 32,
                            borderRadius: "50%",
                            background: fullnessColors[fullness],
                            border: "3px solid #fff",
                            boxShadow: "0 0 0 3px rgba(0,0,0,0.25)",
                          }}
                        />
                        <span
                          style={{
                            background: "rgba(255,255,255,0.92)",
                            padding: "4px 8px",
                            borderRadius: 8,
                            fontSize: 15,
                            fontWeight: 600,
                          }}
                        >
                          {label}
                        </span>
                      </div>
                    </PigeonMarker>
                  );
                })}
              </Map>
            </div>
          </>
        )}
      </PageSection>
    </AppShell>
  );
}
