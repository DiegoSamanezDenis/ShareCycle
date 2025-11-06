import { Map, Marker as PigeonMarker } from "pigeon-maps";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiRequest } from "../api/client";
import type { FullnessCategory, StationSummary } from "../types/station";

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

  return (
    <main>
      <header>
        <h1>ShareCycle</h1>
        <p>
          Find the perfect station, reserve your ride, and start exploring the
          city.
        </p>
        <nav>
          <Link to="/register">Create rider account</Link> ·{" "}
          <Link to="/login">Sign in</Link> ·{" "}
          <Link to="/dashboard">Go to dashboard</Link> ·{" "}
          <Link to="/pricing">View pricing plans</Link>
        </nav>
      </header>

      <section>
        <h2>Explore Stations</h2>
        <p>Guests can browse availability before signing in.</p>
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
                margin: "12px 0",
                fontSize: 16,
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
                      style={{ display: "flex", alignItems: "center", gap: 12 }}
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
          </>
        )}
      </section>
    </main>
  );
}
