## ShareCycle Frontend

This document captures the current state of the ShareCycle frontend

### Scope (Current UI)
- Stories implemented in the UI: 1–3
  - Story 1: Registration (Register rider)
  - Story 2: Login & Session (role-aware access, route guard)
  - Story 3: Station summary (list of stations with basic availability).

### Tech Stack
- React 19 + TypeScript (Vite 7)
- React Router DOM 7
- ESLint (flat config) + Prettier
- Vitest + @testing-library/react (jsdom)

### Project Structure (frontend)
```
src/frontend/
  src/
    api/
      client.ts            # API utility (base URL, auth header, error handling)
    auth/
      AuthContext.tsx      # Auth state (token, role, userId, username); login/logout
    config/
      env.ts               # appConfig.apiUrl → defaults to http://localhost:8080/api
    pages/
      HomePage.tsx
      LoginPage.tsx        # Sign-in form; saves session to storage via AuthContext
      RegisterPage.tsx     # Basic registration form (Rider)
      DashboardPage.tsx    # Simplified station summary table only (Stories 1–3)
      __tests__/
        AuthFlow.test.tsx  # Route guard + login flow
        DashboardPage.test.tsx  # Asserts station overview renders
    routes.tsx             # App routes + RequireAuth wrapper for /dashboard
    App.tsx, main.tsx, index.css
```

### Configuration
- API base URL is defined in `src/frontend/src/config/env.ts`:
  - Defaults to `http://localhost:8080/api`. Adjust if backend runs elsewhere.
- Auth storage key: `sharecycle.auth` (JSON object with token, role, userId, username).

### Authentication & Routing
- `AuthContext.tsx` exposes:
  - `token`, `role`, `userId`, `username`
  - `login(credentials)` and `logout()`
- `routes.tsx` defines routes:
  - `/` → Home
  - `/register` → Register
  - `/login` → Login
  - `/dashboard` → Protected by `RequireAuth` (renders `LoginPage` if no token)

### API Client
- `api/client.ts` provides `apiRequest<T>(path, options)`:
  - Automatically attaches `Authorization` header from explicit `options.token` or `localStorage`.
  - Sets `Content-Type: application/json` for string bodies.
  - Throws errors with server-provided message when possible.

### Dashboard (Story 3)
- `DashboardPage.tsx` shows a Station Overview table only:
  - Fetches `GET /api/stations` via `apiRequest` on mount when a token exists.
  - Renders name, status, bikes docked, free docks, and capacity.

### Tests
- Keep:
  - `AuthFlow.test.tsx` → validates the route guard and a happy-path login navigation.
  - `DashboardPage.test.tsx` → validates that station overview renders with mocked data.
- Removed:
  - `Flows.test.tsx` and all tests for reservations, trips, operator controls, and map rendering.

### Scripts
- From `src/frontend`:
  - `npm run dev` → Vite dev server
  - `npm run build` → type-check + bundle
  - `npm run preview` → serve production build
  - `npm run lint` → ESLint (no warnings allowed by default script)
  - `npm run type-check` → TypeScript no-emit check
  - `npm run test` / `npm run coverage` → Vitest

### How to Run (Frontend Only)
1. `cd src/frontend`
2. `npm install`
3. `npm run dev`
4. Open the printed local URL (typically `http://localhost:5173`). Ensure the backend is running at `http://localhost:8080` (or update `env.ts`).

### Dev Tips
- Manually set an auth session for testing:
  ```js
  localStorage.setItem('sharecycle.auth', JSON.stringify({
    token: 'demo-token', role: 'RIDER', userId: 'u1', username: 'rider1'
  }))
  ```
- If you see “Failed to fetch,” confirm backend is up at `http://localhost:8080/health` and CORS is enabled. Current backend `SecurityConfig` permits `/api/**` with CORS for `http://localhost:5173`.
- Type-only imports: prefer `import type { ReactNode } from 'react'` to satisfy TS 5.9 and ESLint.


### What's left
1. Story 4 – Reservation UI
   - Add a “Reserve bike” form to `DashboardPage.tsx` (or a child component) that POSTs to `/api/reservations` with `riderId`, `stationId`, `bikeId`, and `expiresAfterMinutes`.
   - Display reservation feedback and an expiry countdown.
   - Tests: add a test that mocks POST `/reservations` and validates text feedback.
2. Story 5 – Start Trip UI
   - Add a form to POST `/api/trips` with `riderId`, `bikeId`, `stationId`.
   - Tests: verify feedback appears and overview refreshes.
3. Story 6 – End Trip UI
   - Add a form to POST `/api/trips/:tripId/end` with `stationId`.
   - Tests: verify receipt details appear.
4. Stories 7–9 – Operator Controls
   - Add forms for PATCH `/api/stations/:id/status` and `/api/stations/:id/capacity`, and POST `/api/stations/move-bike`.
   - Ensure buttons are disabled when rules fail; surface error messages.
   - Tests: one test per control with mocked responses.
5. Map 
   - Re-introduce a map to visualize stations. If you use Pigeon Maps again:
     - `npm i pigeon-maps`
     - Render `<Map>` with markers sized for visibility; add a legend.
   - Consider storing real coordinates in the station summary to avoid demo-only placement.

### Coding Conventions
- Components in PascalCase; hooks/utilities in camelCase.
- Keep two-space indents; avoid unrelated reformatting.
- Keep code verbose and readable; avoid overly clever one-liners.
- Add comments only for non-obvious logic or invariants.

### Known Constraints / Assumptions
- Station summary currently does not include real lat/lng; the UI shows a table only (no map).
- Auth is a simple opaque token in `Authorization` header; no refresh token or silent re-auth yet.
- Error messaging is basic (server-provided message preferred).

### Maintenance Checklist
- Before PRs: `npm run lint`, `npm run type-check`, `npm run test`, and consider `npm run coverage`.
- Keep `docs/README.md` and this `docs/FRONTEND.md` updated when features are added.
- Ensure `/api` base URL in `env.ts` matches your backend environment.


