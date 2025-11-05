## ShareCycle Frontend

Updated guide for the React UI that now delivers Stories 1–9 end to end.

### Scope (Current UI)
- Stories 1–9 are demo-ready across the frontend:
  - Story 1: Rider registration with full validation and friendly errors.
  - Story 2: Login/session handling with role-aware dashboards and route guards.
  - Story 3: Station summary list plus map markers coloured by fullness.
  - Story 4: Reservation workflow with countdown, success/failure toasts, and auto-refresh.
  - Story 5: Trip start guardrails and inline feedback when preconditions fail.
  - Story 6: Trip completion/receipt flow showing billing totals.
  - Story 7: Operator controls for status toggles, capacity adjustments, and bike moves.
  - Story 8: Guest landing map + legend with pricing CTA, no destructive actions.
  - Story 9: Station details drawer with dock grid, role-based action buttons, and event console.

### Tech Stack
- React 19 + TypeScript (Vite 7)
- React Router DOM 7
- Zustand-powered auth context (plain React state under the hood)
- ESLint (flat config) + Prettier
- Vitest + Testing Library + MSW for integration-style UI tests
- Pigeon Maps for the station map visualisation

### Project Structure
```
src/frontend/
  src/
    api/
      client.ts              # Shared fetch wrapper (auth headers, safe error parsing)
    auth/
      AuthContext.tsx        # Token/role storage + login/logout helpers
    components/
      (inline in pages today – extracted pieces live alongside dashboard)
    config/
      env.ts                 # appConfig.apiUrl with defaults + type-safe access
    pages/
      HomePage.tsx           # Guest map + legend + pricing CTA
      LoginPage.tsx          # Story 2 login experience
      RegisterPage.tsx       # Story 1 registration form
      DashboardPage.tsx      # All stories 3–9 for riders/operators
      NotFoundPage.tsx
      __tests__/             # Vitest suites (auth, flows, dashboard, guest map)
    routes.tsx               # Router + RequireAuth guard
    types/                   # DTO contracts: stations, events, auth
    App.tsx, main.tsx, index.css
```

### Configuration
- API base URL lives in `src/frontend/src/config/env.ts` and resolves to `import.meta.env.VITE_API_URL ?? "http://localhost:8081/api"`.
- Auth tokens persist under `localStorage` key `sharecycle.auth` (JSON with `token`, `role`, `userId`, `username`).

### Authentication & Routing
- `AuthContext` persists the session, exposes `login`/`logout`, and attaches the bearer token through `apiRequest`.
- `routes.tsx` defines public routes (`/`, `/register`, `/login`) and the protected dashboard. `RequireAuth` redirects unauthenticated visitors to the login page.
- Logout wipes local storage and informs the backend.

### API Client
- `api/client.ts` abstracts fetch:
  - Sets JSON headers when needed and attaches the bearer token.
  - Uses `response.clone().json()` plus a fallback to text/status to preserve error messages from HTML or plain-text responses.
  - Normalises 204 responses to `undefined`.

### Feature Highlights
- **Registration (Story 1):** Field-level validation, inline server errors, and automatic role assignment to Rider.
- **Login & Sessions (Story 2):** Role-aware redirects (rider -> dashboard, operator -> management view) with token persistence and logout.
- **Station Summary + Map (Story 3 & 8):** Combined table and map view showing fullness, status, and tooltips. Legend clarifies colour-coding for guests and authenticated users.
- **Reservations & Trips (Stories 4-6):** Dock-level actions let riders reserve, start, and end trips without typing IDs; countdowns and receipts refresh automatically after each step, and the active trip snapshot survives a page refresh via local storage.
- **Operator Controls (Story 7):** Status toggles, capacity adjustments, and bike moves surface only for operators with disabled states when rules fail.
- **Station Details Panel (Story 9):** Dock grid shows live occupancy with inline controls (map markers and table rows open the same panel), and the event console polls `/api/public/events` to highlight backend activity.

### Tests
- `src/App.test.tsx` – smoke test for routing/shell.
- `src/pages/__tests__/AuthFlow.test.tsx` – login guard, redirects, logout flow.
- `src/pages/__tests__/DashboardPage.test.tsx` – station overview, map legend, drawer rendering.
- `src/pages/__tests__/Flows.test.tsx` – rider reservation/start/end flow and operator controls hitting mocked endpoints.
- `src/pages/__tests__/HomePage.test.tsx` – guest landing map + legend with pricing CTA.
- All tests run via `npm test` (Vitest in CI mode).

### Scripts
- `npm run dev` – Vite dev server.
- `npm run build` – TypeScript build + production bundle.
- `npm run preview` – Serve production build locally.
- `npm run lint` – ESLint.
- `npm run type-check` – TypeScript `--noEmit` pass.
- `npm run test` / `npm run coverage` – Vitest suites.

### How to Run (Frontend)
1. `cd src/frontend`
2. `npm install`
3. `npm run dev`
4. Open `http://localhost:5173` (ensure backend reachable at the URL defined in `env.ts` or `.env`).

### Dev Tips
- To simulate auth quickly in the browser console:
  ```js
  localStorage.setItem('sharecycle.auth', JSON.stringify({
    token: 'demo-token',
    role: 'RIDER',
    userId: 'rider-1',
    username: 'rider@example.com',
  }));
  ```
- When testing operator flows, seed credentials: `SmoothOperator` / `wowpass` (created by backend seeder).
- If fetch calls fail, confirm the backend health endpoint (`/api/health` or `/health` depending on profile) and CORS configuration in `SecurityConfig`.
- Active trips persist in `localStorage` under `sharecycle.activeTrip`; clear it if you need to reset the rider state between demos.

### Known Constraints
- Map uses seeded coordinates; adjust `ListStationSummariesUseCase` payload or seed data for different demos.
- UI polls events every 10 seconds; for large loads consider SSE/websocket upgrade.
- Payment display is a simple ledger total; full invoice history is future work.
- CSS relies on module-level styles in `DashboardPage.tsx`; extracting components could improve reuse, but not required for demo readiness.
