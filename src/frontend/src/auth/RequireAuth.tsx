import type { ReactElement } from "react";
import { useAuth } from "./AuthContext";
import LoginPage from "../pages/LoginPage";

type RequireAuthProps = {
  children: ReactElement;
};

export function RequireAuth({ children }: RequireAuthProps) {
  const { token } = useAuth();
  if (!token) {
    return <LoginPage />;
  }
  return children;
}
