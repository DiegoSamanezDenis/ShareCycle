# ShareCycle – Developer Guide

This document covers both frontend (React + TypeScript) and backend (Spring Boot) setup, how they fit together, and how to run everything locally.

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
- Copy `.env.example` to `.env` (or `.env.local`) and set `VITE_API_URL` to your backend, e.g. `http://localhost:8080/api`.
- The app reads `import.meta.env.VITE_API_URL` and falls back to `http://localhost:8080/api` (see `src/frontend/src/config/env.ts`).
- If your backend does not serve under `/api`, use `http://localhost:8080`.

## Scripts
- `npm run dev` — start Vite dev server at http://localhost:5173
- `npm run type-check` — TypeScript check (no emit)
- `npm run lint` — ESLint with zero‑warning enforcement
- `npm run test` — Vitest in CI mode
- `npm run test:watch` — Vitest watch mode
- `npm run coverage` — Vitest with V8 coverage
- `npm run build` — `tsc -b` followed by `vite build`
- `npm run preview` — serve the production bundle locally

## Project Layout (src/frontend)
- `index.html` — Vite HTML entry point
- `package.json` — scripts and dependencies
- `vite.config.ts` — Vite + Vitest configuration
- `tsconfig*.json` — TypeScript compiler settings
- `eslint.config.js` — Flat‑config ESLint setup
- `src/main.tsx` — React entry point
- `src/App.tsx` — router host component
- `src/index.css` — global styles
- `src/config/env.ts` — typed access to environment variables
- `src/pages/` — route placeholders (Home, Register, Login, Dashboard, NotFound)
- `src/test/setup.ts` — Testing Library + jest‑dom hooks

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

The backend is a standard Boot app exposing REST endpoints (e.g., `GET /health`). JPA is configured; schema is managed via Flyway SQL migrations.

## Project Layout (src/backend)
- `src/main/java/com/sharecycle/SharecycleApplication.java` — Spring Boot entry point
- `src/main/java/com/sharecycle/ui/HealthController.java` — health endpoint
- `src/main/resources/application.yml` — default config (MySQL + Flyway)
- `src/main/resources/application-local.yml` — local dev profile (H2, Flyway off)
- `src/main/resources/db/migration/` — Flyway SQL migrations (V1, V2)
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
- Health check: `curl http://localhost:8080/health` → `ok`
- Stop: `docker compose down` (add `-v` to wipe DB volume)

Option B — Backend only (no Docker; H2 profile)
```
cd src/backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# Windows: mvnw.cmd ...
```
- Health check: `curl http://localhost:8080/health`

Option C — DB in Docker, backend on host JVM
```
cd src && docker compose up -d db
cd ../backend && ./mvnw spring-boot:run
```

## Build and Test
- Build without tests: `cd src/backend && ./mvnw -DskipTests package`
- Run tests: `cd src/backend && ./mvnw test` (uses `test` profile)

## Flyway Migrations
- Location: `src/backend/src/main/resources/db/migration`
- Examples:
  - `V1_baseline.sql` — baseline marker
  - `V2_create_users.sql` — creates `users` table (UUID as `BINARY(16)`, unique email/username)
- Runs on startup in default profile; disabled in `local` and `test`.

## Troubleshooting
- Docker on Windows: start Docker Desktop; ensure Linux containers; retry compose.
- Flyway “Unsupported Database: MySQL 8.0”: ensure `flyway-mysql` is present (in `pom.xml`).
- Port in use (3306/8080): change mappings in `src/docker-compose.yml` or stop the conflicting process.
- Long‑running logs: servers keep running; stop with Ctrl+C or use IDE Run/Debug.
- CORS during dev: if calling backend from http://localhost:5173, add a simple CORS config or proxy via Vite.

## Useful Endpoints
- `GET /health` — returns `ok`

