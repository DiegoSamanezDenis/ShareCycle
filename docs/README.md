# ShareCycle – Developer Guide

This document covers both frontend (React + TypeScript) and backend (Spring Boot) setup, how they fit together, and how to run everything locally.

---

# Developer Quick Start

1) Clone and bootstrap
```
git clone <repo>
cd ShareCycle
# Frontend deps
cd src/frontend && npm install
```

2) Run everything with Docker (DB + API)
```
cd src
docker compose up -d --build
# Health check
curl http://localhost:8081/health
```

3) Run the frontend
```
cd src/frontend
npm run dev
# App at http://localhost:5173
```

4) Local-only backend (H2, no Docker)
```
cd src/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Windows: mvnw.cmd ...
```

## Prerequisites
- Java 21 (JDK) for the backend
- Node.js 18+ and npm 9+ for the frontend
- Docker Desktop (Linux containers) to run MySQL and optionally the backend

## Project Structure
- Frontend: `src/frontend` (Vite + React Router + Vitest)
- Backend: `src/backend` (Spring Boot Web, Validation, Data JPA, Flyway)
- Compose: `src/docker-compose.yml` (MySQL and backend service)

---

# Frontend Guide

## Install
```
cd src/frontend
npm install
```

## Environment
- Copy `.env.example` to `.env` (or `.env.local`) and set `VITE_API_URL` to your backend, e.g. `http://localhost:8081/api`.
- The app reads `import.meta.env.VITE_API_URL` and falls back to `http://localhost:8081/api` (see `src/frontend/src/config/env.ts`).
- If your backend does not serve under `/api`, use `http://localhost:8081`.

## Scripts
- `npm run dev` — start Vite dev server at http://localhost:5173
- `npm run type-check` — TypeScript check (no emit)
- `npm run lint` — ESLint with zero‑warning enforcement
- `npm run test` — Vitest in CI mode
- `npm run test:watch` — Vitest watch mode
- `npm run coverage` — Vitest with V8 coverage
- `npm run build` — `tsc -b` followed by `vite build`
- `npm run preview` — serve the production bundle locally

### Frontend API usage
- Base URL: `VITE_API_URL` (defaults to `http://localhost:8081/api`)
- Auth header: backend expects an `Authorization` header with an opaque token returned by login
- Example
```
await apiRequest('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) })
await apiRequest('/stations', { token })
```

## Project Layout (src/frontend)
- `index.html` — Vite HTML entry point
- `package.json` — scripts and dependencies
- `vite.config.ts` — Vite + Vitest configuration
- `tsconfig*.json` — TypeScript compiler settings
- `eslint.config.js` — Flat‑config ESLint setup
- `src/main.tsx` — React entry point
- `src/App.tsx` - router host component
- `src/index.css` - global styles
- `src/config/env.ts` - typed access to environment variables
- `src/api/` - shared `apiRequest` client with auth header + error handling
- `src/auth/` - `AuthContext` for token persistence, login/logout helpers, and guards
- `src/pages/` - feature-complete pages (Home map, Register, Login, Dashboard with rider/operator flows, NotFound) plus colocated tests
- `src/types/` - DTO contracts shared across pages/tests
- `src/test/setup.ts` - Testing Library + jest-dom hooks

## Testing
- Vitest runs with a jsdom environment and globals from `vitest/globals`.
- @testing-library/react renders components; @testing-library/jest-dom adds matchers.

---

# Backend Guide

## Overview
- Runtime: Spring Boot 3.5 (Java 21)
- Build: Maven wrapper in `src/backend/mvnw(.cmd)`
- Database: MySQL 8 via Docker Compose, Flyway migrations
- Local Dev Option: H2 in‑memory profile (`local`) disables Flyway
- Schedulers: `ReservationExpiryScheduler` runs every minute to expire reservations and persist bikes back to `AVAILABLE`.

The backend is a standard Boot app exposing REST endpoints (e.g., `GET /health`). JPA is configured; schema is managed via Flyway SQL migrations.

## Project Layout (src/backend)
- `src/main/java/com/sharecycle/SharecycleApplication.java` — Spring Boot entry point
- `src/main/java/com/sharecycle/ui/HealthController.java` — health endpoint
- `src/main/java/com/sharecycle/application/BmsFacade.java` - facade orchestrating BMS workflows
- Controllers (JSON APIs under `/api/...`):
  - `RegistrationController` (`/api/auth/register`)
  - `LoginController` (`/api/auth/login`, `/api/auth/logout`)
  - `ReservationController` (`/api/reservations`)
  - `TripController` (`/api/trips`, `/api/trips/{tripId}/end`)
  - `StationController` (`/api/stations`, status/capacity/move-bike)
- `src/main/resources/application.yml` — default config (MySQL + Flyway)
- `src/main/resources/application-local.yml` — local dev profile (H2, Flyway off)
- `src/main/resources/db/migration/` — Flyway SQL migrations (see list below)
- `src/test/resources/application-test.yml` — test profile (H2, Flyway off)
- `pom.xml` — web, validation, data‑jpa, MySQL driver, Flyway (core + mysql), H2

## How Boot, Maven, Docker, and MySQL fit together
- Maven builds and runs the Boot app via the Spring Boot Maven Plugin.
- Default profile connects to MySQL using datasource in `application.yml`.
- `src/docker-compose.yml` provides MySQL and a backend service container.
- Flyway runs automatically on startup (default profile) and applies SQL migrations under `classpath:db/migration`.
- Profiles:
  - Default (no profile): MySQL + Flyway
  - `local`: H2 in‑memory; Flyway disabled (fast local dev; no Docker)
  - `test`: H2 in‑memory; Flyway disabled (tests)

## Database Configuration
- Defaults (from `application.yml`):
  - `spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/sharecycle?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}`
  - `spring.datasource.username=${DB_USER:sharecycle}`
  - `spring.datasource.password=${DB_PASSWORD:devpw}`
- Override with environment variables:
  - PowerShell: `setx DB_URL "jdbc:mysql://localhost:3306/sharecycle?..."`
  - Bash/zsh: `export DB_URL=jdbc:mysql://localhost:3306/sharecycle?...`
- Example template: `src/backend/.env.example` (reference only; Boot does not auto‑load `.env`).

## Run Scenarios

Option A — One command (DB + API via Docker Compose)
```
cd src
docker compose up -d --build
```
- Health check: `curl http://localhost:8081/health` → `ok`
- Stop: `docker compose down` (add `-v` to wipe DB volume)

Option B — Backend only (no Docker; H2 profile)
```
cd src/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Windows: mvnw.cmd ...
```
- Health check: `curl http://localhost:8081/health`

Option C — DB in Docker, backend on host JVM
```
cd src && docker compose up -d db
cd ../backend && ./mvnw spring-boot:run
```

## Build and Test
- Build without tests: `cd src/backend && ./mvnw -DskipTests package`
- Run tests: `cd src/backend && ./mvnw test` (uses `test` profile)

### Test Playbook (after making changes)
Run these before committing significant changes:
- `cd src/backend && ./mvnw test` — runs H2-based integration tests for use cases and controllers
- `cd src/frontend && npm run type-check && npm run lint && npm run test && npm run build`
- Optional coverage gate (frontend): `npm run coverage`

Notes on controller tests:
- Spring Security is enabled by default; POST endpoints may require CSRF tokens or security to be disabled in test slices if you add auth middleware.
- If you add security, consider adding `.with(csrf())` to POSTs and configuring a test security config.

## Flyway Migrations
- Location: `src/backend/src/main/resources/db/migration`
- Present:
  - `V1__baseline.sql` – baseline marker
  - `V2__create_users.sql` – creates `users` table (UUID as `BINARY(16)`, unique email/username)
  - `V3__create_station_dock_bike_tables.sql` – station/dock/bike tables
  - `V4__add_bike_station_fk.sql` – links bikes to their current station
  - `V5__create_reservation_trip_ledger.sql` – reservation, trip, ledger tables
  - `V6__add_bill_columns_to_ledger.sql` – adds billing breakdown (pricing_plan, bill_id, bill_computed_at, base_cost, time_cost, ebike_surcharge, total_cost); drops old total_amount column; adds check constraints and indexes for analytics
- Runs on startup in default profile; disabled in `local` and `test`.

## Troubleshooting
- Docker on Windows: start Docker Desktop; ensure Linux containers; retry compose.
- Flyway “Unsupported Database: MySQL 8.0”: ensure `flyway-mysql` is present (in `pom.xml`).
- Port in use (3306/8081): change mappings in `src/docker-compose.yml` or stop the conflicting process.
- Long‑running logs: servers keep running; stop with Ctrl+C or use IDE Run/Debug.
- CORS during dev: if calling backend from http://localhost:5173, add a simple CORS config or proxy via Vite.

## Useful Endpoints
- `GET /health` — returns `ok`
- `POST /api/auth/register` — register rider
- `POST /api/auth/login` — login; returns `{ userId, username, role, token }`
- `POST /api/auth/logout` — invalidates token
- `GET /api/stations` — list station summaries
- `PATCH /api/stations/{stationId}/status` — toggle station status
- `PATCH /api/stations/{stationId}/capacity` — adjust station capacity
- `POST /api/stations/move-bike` — move a bike between stations
- `POST /api/reservations` — create reservation
- `POST /api/trips` — start trip
- `POST /api/trips/{tripId}/end` — end trip and bill

### Endpoint contracts (request/response shapes)
- POST `/api/auth/login`
  - Request: `{ username: string, password: string }`
  - Response: `{ userId: UUID, username: string, role: 'RIDER'|'OPERATOR', token: string }`
- POST `/api/auth/register`
  - Request: `{ fullName, streetAddress, email, username, password, paymentMethodToken }`
  - Response: `{ userId, username, role, email, fullName }`
- GET `/api/stations`
  - Response: `Array<{ stationId, name, status, bikesDocked, capacity, freeDocks }>`
- PATCH `/api/stations/{id}/status`
  - Request: `{ operatorId: UUID, outOfService: boolean }`
- PATCH `/api/stations/{id}/capacity`
  - Request: `{ operatorId: UUID, delta: number }`
- POST `/api/stations/move-bike`
  - Request: `{ operatorId: UUID, bikeId: UUID, destinationStationId: UUID }`
- POST `/api/reservations`
  - Request: `{ riderId: UUID, stationId: UUID, bikeId: UUID, expiresAfterMinutes: number }`
  - Response: `{ reservationId, stationId, bikeId, reservedAt, expiresAt, active }`
- POST `/api/trips`
  - Request: `{ tripId?: UUID|null, riderId: UUID, bikeId: UUID, stationId: UUID, startTime?: ISO8601|null }`
  - Response: `{ tripId, stationId, bikeId, riderId, startedAt }`
- POST `/api/trips/{tripId}/end`
  - Request: `{ stationId: UUID }`
  - Response: `{ tripId, endStationId, endedAt, durationMinutes, ledgerId, totalAmount }`

## Frontend UI Demo

- Tech: React 19 + Vite + React Router, Testing Library + Vitest, Pigeon Maps (map component compatible with React 19).
- Location: `src/frontend`

### Run (frontend)
```
cd src/frontend
npm install
npm run dev
# App at http://localhost:5173
```

Set `VITE_API_URL` if needed (defaults to `http://localhost:8081/api`).

### Features
- Auth: Register, Login, Logout (token stored in localStorage)
- Route guard: `/dashboard` requires login
- Dashboard:
  - Station summaries table
  - Rider actions: Reserve bike (with countdown), Start trip, End trip (receipt)
  - Operator controls: Toggle status, Adjust capacity, Move bike
  - Map + legend (Pigeon Maps) and Station details panel
  - Event console (recent actions)

### Tests (frontend)
```
cd src/frontend
npm run type-check && npm run lint && npm run test
```
Current suite: routing, auth guard/login, dashboard loads, reservation, trip start/end, operator controls.

## Full Local Run (DB + API + Frontend)

Option A — Docker (recommended)
```
# From repo root
cd src
docker compose up -d --build
# Health check
curl http://localhost:8081/health
# In another terminal
cd ../frontend
npm install
npm run dev
# Open http://localhost:5173
```

Option B — Backend only with H2 (no Docker)
```
cd src/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Windows: mvnw.cmd ...
cd ../../frontend
npm install
npm run dev
```

## Demo Accounts & Seed Data

- Operator (auto-seeded on startup via `DataSeeder` in non-test profiles):
  - Username: `SmoothOperator`
  - Password: `wowpass`
  - Role: `OPERATOR`

- Rider: create one via the Register page (no pre-seeded rider by default). Use any email/username; a token will be returned on login.

- Stations/Bikes: pre-seeded under `src/backend/src/main/resources/db/data/` with Montreal coordinates (e.g., “Station #1”, “Station #2”, …). Use dashboard actions to reserve, start, and end trips.

### Story-by-Story Demo Guide (Stories 1-7)
1. **Story 1 – Rider Registration**
   - Navigate to `/register` from the landing page.
   - Fill in name, address, email, username, password, and any mock payment token.
   - Submit and point out inline validations (invalid email, duplicate username, etc.).
   - Show the success toast and the redirect prompt to login (credentials persist hashed server-side).
2. **Story 2 – Login & Session**
   - Visit `/login`, enter the newly created rider credentials, and sign in.
   - Highlight the issued token in local storage (`sharecycle.auth`) and the redirect to the rider dashboard.
   - Briefly show that accessing `/dashboard` without a token now redirects to `/login` (route guard).
3. **Story 3 - Station Summaries**
   - On the rider dashboard, point out the station table (status, bikes available, free docks, capacity).
   - Switch to the map view to show markers coloured by fullness; hover a marker to display the tooltip and click it to open the same station in the details panel.
   - Mention that data comes from the backend seeder and updates live as actions occur.
4. **Story 4 - Reserve Bike**
   - From the station list or by clicking a map marker, open the station details panel.
   - Pick a dock with an available bike and press the inline `Reserve` button.
   - Show the success notification, updated availability counts, and the countdown timer until expiry.
   - (Optional) Demonstrate reservation validation by attempting a second reservation while one is active.
5. **Story 5 - Start Trip**
   - With an active reservation, click `Start trip` beside the reserved bike.
   - Point out that other bikes stay disabled while the reservation is active.
   - Point out the dashboard updates: reservation cleared, trip banner displayed, event console logging `TripStarted`.
   - Refresh the dashboard to show that the active trip banner persists (stored locally) until the ride is finished.
6. **Story 6 - End Trip**
   - Choose a destination station with free docks and press `End trip here`.
   - Highlight the receipt panel with billing total and the station counts adjusting.
   - Mention that the backend ledger entry is created and reflected in the event console (`TripEnded`, `TripBilled`).
7. **Story 7 – Operator Station Management**
   - Log out (`/logout` or header action) and re-login as the seeded operator (`SmoothOperator` / `wowpass`).
   - In the operator dashboard view, open the station drawer:
     - Toggle status to `OUT_OF_SERVICE`, note how rider actions disable and the event console logs the change.
     - Adjust capacity using `+1 Dock` / `-1 Dock`, pointing out the capacity count and events.
     - Use `Move bike here` to transfer a bike from a source station; show source/destination counts updating and validation errors if rules break.
   - Close by emphasising role-based controls (riders never see operator buttons, guests stay read-only).

## Operator Station Operations (new)
- **Use cases** live under `com.sharecycle.application`:
  - `SetStationStatusUseCase` toggles stations between active/out-of-service. Only operators may call `execute`.
  - `AdjustStationCapacityUseCase` adds or removes docks. Positive `delta` creates empty docks; negative `delta` removes empty docks (fails if not enough free docks).
  - `MoveBikeUseCase` rebalances bikes across stations. Validates operator role, bike availability, source/destination status, and free dock before moving.
- **Domain updates**:
  - `Station` derives capacity from its dock collection, offers helpers (`addEmptyDocks`, `removeEmptyDocks`, `dockBike`, `undockBike`) to keep counts legal.
  - `Dock` exposes `isEmpty()`; `Bike` tracks `currentStation` through a nullable FK (added in `V4__add_bike_station_fk.sql`).
- **Events** (`com.sharecycle.domain.event`) fire on success so dashboards can refresh:
  - `StationStatusChangedEvent`, `StationCapacityChangedEvent`, `BikeMovedEvent`.
- **Testing**: run `./mvnw test` (H2 + `ddl-auto=create-drop`) to execute new integration suites covering station status, capacity, and move workflows.
- **API surface (implemented)**: controller layer forwards PATCH/POST calls to these use cases and returns clear 4xx messages on validation errors (e.g., “Destination station has no free docks.”). See `StationController` for routes.

---

# Reservation & Trips (new)

- **Use cases** live under `com.sharecycle.application`:
  - `ReserveBikeUseCase` validates preconditions, marks bike `RESERVED`, persists, publishes `ReservationCreated`.
  - `ReservationExpiryScheduler` runs periodically to expire reservations and persist bikes back to `AVAILABLE`.
  - `StartTripUseCase` undocks from start station, marks bike `ON_TRIP`, persists trip/station/bike, publishes `TripStarted`.
  - `EndTripUseCase` docks at end station, marks bike `AVAILABLE`, ends trip via `TripBuilder`, persists ledger, publishes `TripEnded`/`TripBilled`.
- **API surface**:
  - `POST /api/reservations` — create reservation
  - `POST /api/trips` — start trip
  - `POST /api/trips/{tripId}/end` — end trip and bill

---

# Codebase Layout & Where to Find Things

Backend (Spring Boot)
- Application (use cases, orchestration): `src/backend/src/main/java/com/sharecycle/application`
  - Reserve/Start/End Trip, Station controls, Scheduler
- UI Controllers (REST): `src/backend/src/main/java/com/sharecycle/ui`
- Domain (models, builders, state): `src/backend/src/main/java/com/sharecycle/domain`
- Infrastructure (JPA adapters/entities, events): `src/backend/src/main/java/com/sharecycle/infrastructure`
- Config & profiles: `src/backend/src/main/resources/*.yml`
- Migrations: `src/backend/src/main/resources/db/migration`
- Tests: `src/backend/src/test/java/...`

Frontend (Vite + React)
- Pages (routes): `src/frontend/src/pages`
- Auth/session: `src/frontend/src/auth/AuthContext.tsx`
- API client: `src/frontend/src/api/client.ts`
- App & router: `src/frontend/src/App.tsx`, `src/frontend/src/routes.tsx`
- Tests & setup: `src/frontend/src/App.test.tsx`, `src/frontend/src/test/setup.ts`

---

# Security & Auth
- Login returns an opaque `token`; send it in `Authorization` header on subsequent requests.
- Registration and login endpoints are under `/api/auth/...`.
- For local dev, you can run without strict controller-level security; when enabling security, ensure POST requests include CSRF tokens or configure test security accordingly.

---

# Collaboration Tips (FE <> BE)
- Agree on base URL via `.env` (`VITE_API_URL`), default is `http://localhost:8081/api`.
- The “Endpoint contracts” section above enumerates payloads and responses; extend it when adding endpoints.
- Use the domain events (logged in API) to trace reservation/trip/station flows during manual testing.


