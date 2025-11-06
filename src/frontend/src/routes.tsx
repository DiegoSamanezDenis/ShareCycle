//src/routes.tsx
import type { RouteObject } from "react-router-dom";
import DashboardPage from "./pages/DashboardPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import NotFoundPage from "./pages/NotFoundPage";
import RegisterPage from "./pages/RegisterPage";
import TripSummaryPage from "./pages/TripSummaryPage";

import { useAuth } from "./auth/AuthContext";


function RequireAuth({ children }: { children: JSX.Element }) {
  const { token } = useAuth();
  if (!token) {
    return <LoginPage />;
  }
  return children;
}

export const routes: RouteObject[] = [
  { path: "/", element: <HomePage /> },
  { path: "/register", element: <RegisterPage /> },
  { path: "/login", element: <LoginPage /> },
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
