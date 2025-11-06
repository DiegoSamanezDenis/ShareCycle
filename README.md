# ShareCycle
- Pritthiraj Dey, 40273416, dpritthiraj@gmail.com, github: rajdey03, Full-Stack
- Diego Samanez Denis, 40286385, dsamanezdenis@gmail.com, github: DiegoSamanezDenis, Backend
- Minh Huy Tran, 40263743, willhuy03@gmail.com, github: valvatorezbraveheart, Backend
- Huu Khoa Kevin Tran, 40283037, t_huukho@live.concordia.ca, github: hkevint, Full-Stack
- Bhaskar Das, 40325270, bh_das@live.concordia.ca, github: das-bhaskar, Full-Stack
- Leon Kojakian, 40282267, l_kojak@live.concordia.ca, github: leonlolleonlol, Backend

ShareCycle is a bike sharing application providing docking stations and bikes available for rental. Payment is possible through the dashboard UI and operators are regularly notified with bike usage. Thus, they able to rebalance the docking stations to ensure proper availability to all users.

## Setup Backend
- Create Stripe Account
- In `src/backend/src/main/resources` add a `.env.payment` file
- In `.env.payment` file add `STRIPE_SECRET_KEY=[API_KEY]` where `[API_KEY]` can be found in your Stripe dev dashboard


## Quick Start (Real App)
- Start DB + API: `cd src && docker compose up -d --build`
- Verify API: open `http://localhost:8081/health` → should return `ok`
- Frontend: `cd src/frontend && cp .env.example .env` then set `VITE_API_URL=http://localhost:8081`
- Install + run: `npm install && npm run dev` → open `http://localhost:5173`
- Stop: Ctrl+C (frontend). To stop backend/DB: `cd ../.. && cd src && docker compose down`

Notes
- Uses real MySQL (not tests). Credentials are defined in `src/docker-compose.yml`.
- If port 3306 or 8081 is occupied, change the mapping in `src/docker-compose.yml`.
- For a production-like frontend bundle: `npm run build && npm run preview` (serves at `http://localhost:4173`).

## Backend Quickstart
- Full guide: see `docs/README.md` (Backend Guide section)
- One command (from `src/`): `docker compose up -d --build`
  - Starts MySQL and the Spring Boot API at `http://localhost:8081`
- Local (no Docker): `cd src/backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`

## Prerequisites
- Java 21 (for backend)
- Node.js 18+ and npm 9+ (for frontend)
- Docker Desktop (to run MySQL and optionally the backend)

## Dev Commands

Frontend
- Install: `cd src/frontend && npm install`
- Dev server: `npm run dev` (http://localhost:5173)

Backend
- Local (H2, no Docker): `cd src/backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
- With MySQL via Docker Compose: `cd src && docker compose up -d --build`

Database
- Start only DB: `cd src && docker compose up -d db`
- Stop services: `cd src && docker compose down` (add `-v` to reset data)
