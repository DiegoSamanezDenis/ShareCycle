import type { RouteObject } from 'react-router-dom';
import DashboardPage from './pages/DashboardPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import NotFoundPage from './pages/NotFoundPage';
import RegisterPage from './pages/RegisterPage';
import { useAuth } from './auth/AuthContext';
import type {JSX} from "react";

// eslint-disable-next-line react-refresh/only-export-components
function RequireAuth({ children }: { children: JSX.Element }) {
  const { token } = useAuth();
  if (!token) {
    return <LoginPage />;
  }
  return children;
}

export const routes: RouteObject[] = [
  { path: '/', element: <HomePage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/dashboard', element: (
    <RequireAuth>
      <DashboardPage />
    </RequireAuth>
  ) },
  { path: '*', element: <NotFoundPage /> }
];

