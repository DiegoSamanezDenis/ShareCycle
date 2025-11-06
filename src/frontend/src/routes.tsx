import React from "react";
import type { RouteObject } from "react-router-dom";
import { useAuth } from "./auth/AuthContext";
import DashboardPage from "./pages/DashboardPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import NotFoundPage from "./pages/NotFoundPage";
import PricingPage from "./pages/PricingPage";
import RegisterPage from "./pages/RegisterPage";
function RequireAuth({ children }: { children: React.ReactElement }) {
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
  { path: "/pricing", element: <PricingPage /> },
  {
    path: "/dashboard",
    element: (
      <RequireAuth>
        <DashboardPage />
      </RequireAuth>
    ),
  },
  { path: "*", element: <NotFoundPage /> },
];
