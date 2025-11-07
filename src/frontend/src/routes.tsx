//src/routes.tsx
import type { RouteObject } from "react-router-dom";
import { RequireAuth } from "./auth/RequireAuth";
import DashboardPage from "./pages/DashboardPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import NotFoundPage from "./pages/NotFoundPage";
import PricingPage from "./pages/PricingPage";
import RegisterPage from "./pages/RegisterPage";
import TripSummaryPage from "./pages/TripSummaryPage";

// using RequireAuth from ./auth/RequireAuth

export const routes: RouteObject[] = [
  { path: "/", element: <HomePage /> },
  { path: "/register", element: <RegisterPage /> },
  { path: "/login", element: <LoginPage /> },
  { path: "/pricing", element: <PricingPage /> },
  {
    path: "/dashboard",
    element: (
      <RequireAuth>
        <DashboardPage />
      </RequireAuth>
    ),
  },

  {
  path: "/trip-summary",
  element: (
    <RequireAuth>
      <TripSummaryPage />
    </RequireAuth>
  ),
},


  { path: "*", element: <NotFoundPage /> },
];
