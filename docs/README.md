# ShareCycle – Frontend Guide

## Prerequisites
- Node.js 18+
- npm 9+

## Install
`ash
cd src/frontend
npm install
`

## Environment
- Copy .env.example to .env (or .env.local) and set VITE_API_URL to your backend, e.g. http://localhost:8080/api.
- The app reads import.meta.env.VITE_API_URL and falls back to http://localhost:8080/api (see src/config/env.ts).

## Scripts
- 
pm run dev — start the Vite dev server at http://localhost:5173
- 
pm run type-check — run TypeScript project references in no-emit mode
- 
pm run lint — ESLint with zero-warning enforcement
- 
pm run test — Vitest in CI mode
- 
pm run test:watch — Vitest watch mode
- 
pm run coverage — Vitest with V8 coverage output
- 
pm run build — 	sc -b followed by ite build
- 
pm run preview — serve the production bundle locally

## Project Layout (src/frontend)
- index.html — Vite HTML entry point
- package.json — scripts and dependency manifest
- ite.config.ts — Vite + Vitest configuration
- 	sconfig*.json — TypeScript compiler settings
- slint.config.js — Flat-config ESLint setup
- src/main.tsx — React entry point
- src/App.tsx — router host component
- src/index.css — global styles
- src/config/env.ts — typed access to environment variables
- src/pages/ — top-level route placeholders (Home, Register, Login, Dashboard, NotFound)
- src/test/setup.ts — Testing Library + jest-dom hooks
- public/vite.svg — favicon used by Vite template

## Testing
- Vitest runs with a jsdom environment and global APIs from itest/globals.
- @testing-library/react renders components, and @testing-library/jest-dom adds DOM matchers.
- Additional helpers can be added via src/test/setup.ts.

## Conventions
- Route components are minimal placeholders; build features incrementally from these stubs.
- Keep React components, styles, and tests colocated in src/.
- Fix lint and type errors before committing.
- Run 
pm run test and 
pm run build prior to opening a pull request.
- Use feature branches and descriptive commit messages.

