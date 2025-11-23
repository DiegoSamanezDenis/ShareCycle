/* eslint-disable react-refresh/only-export-components */
import type { ReactNode } from "react";
import {
    createContext,
    useCallback,
    useContext,
    useMemo,
    useState,
} from "react";
import { apiRequest } from "../api/client";

type AuthRole = "RIDER" | "OPERATOR";

type AuthState = {
  token: string | null;
  role: AuthRole | null;
  userId: string | null;
  username: string | null;
  currentMode: AuthRole | null; // For operators: their active mode
};

type LoginPayload = {
  token: string;
  role: AuthRole;
  userId: string;
  username: string;
  currentMode?: AuthRole;
};

type AuthContextValue = AuthState & {
  login: (payload: LoginPayload) => void;
  logout: () => Promise<void>;
  toggleRole: () => Promise<void>;
  effectiveRole: AuthRole | null; // The role to use for UI (considers currentMode)
};

const STORAGE_KEY = "sharecycle.auth";
const ACTIVE_TRIP_STORAGE_KEY = "sharecycle.activeTrip";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const emptyState: AuthState = {
  token: null,
  role: null,
  userId: null,
  username: null,
  currentMode: null,
};

function loadInitialState(): AuthState {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as AuthState;
      return {
        token: parsed.token ?? null,
        role: parsed.role ?? null,
        userId: parsed.userId ?? null,
        username: parsed.username ?? null,
        currentMode: parsed.currentMode ?? parsed.role ?? null,
      };
    }
  } catch {
    // ignore corrupted persisted state
  }
  return emptyState;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>(loadInitialState);

  const login = useCallback((payload: LoginPayload) => {
    const nextState: AuthState = {
      token: payload.token,
      role: payload.role,
      userId: payload.userId,
      username: payload.username,
      currentMode: payload.currentMode ?? payload.role,
    };
    setState(nextState);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
  }, []);

  const logout = useCallback(async () => {
    if (state.token) {
      try {
        await apiRequest<void>("/auth/logout", {
          method: "POST",
          token: state.token,
        });
      } catch {
        // swallow logout errors
      }
    }
    setState(emptyState);
    localStorage.removeItem(STORAGE_KEY);
    localStorage.removeItem(ACTIVE_TRIP_STORAGE_KEY);
  }, [state.token]);

  const toggleRole = useCallback(async () => {
    if (!state.token || state.role !== "OPERATOR") {
      return;
    }
    try {
      const response = await apiRequest<{
        userId: string;
        username: string;
        baseRole: string;
        currentMode: AuthRole;
        token: string;
      }>("/auth/toggle-role", {
        method: "POST",
        token: state.token,
      });
      console.log("Toggle role response:", response);
      console.log("Current state before update:", state);
      const nextState: AuthState = {
        ...state,
        currentMode: response.currentMode,
      };
      console.log("Next state after update:", nextState);
      setState(nextState);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
    } catch (error) {
      console.error("Failed to toggle role:", error);
      throw error;
    }
  }, [state]);

  const effectiveRole = useMemo<AuthRole | null>(() => {
    // For operators, use currentMode; for others, use role
    if (state.role === "OPERATOR" && state.currentMode) {
      return state.currentMode;
    }
    return state.role;
  }, [state.role, state.currentMode]);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      logout,
      toggleRole,
      effectiveRole,
    }),
    [state, login, logout, toggleRole, effectiveRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
