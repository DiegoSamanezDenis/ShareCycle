/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { apiRequest } from '../api/client';

type AuthRole = 'RIDER' | 'OPERATOR';

type AuthState = {
  token: string | null;
  role: AuthRole | null;
  userId: string | null;
  username: string | null;
};

type LoginPayload = {
  token: string;
  role: AuthRole;
  userId: string;
  username: string;
};

type AuthContextValue = AuthState & {
  login: (payload: LoginPayload) => void;
  logout: () => Promise<void>;
};

const STORAGE_KEY = 'sharecycle.auth';

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const emptyState: AuthState = {
  token: null,
  role: null,
  userId: null,
  username: null
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
        username: parsed.username ?? null
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
      username: payload.username
    };
    setState(nextState);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(nextState));
  }, []);

  const logout = useCallback(async () => {
    if (state.token) {
      try {
        await apiRequest<void>('/auth/logout', {
          method: 'POST',
          token: state.token
        });
      } catch {
        // swallow logout errors
      }
    }
    setState(emptyState);
    localStorage.removeItem(STORAGE_KEY);
  }, [state.token]);

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      login,
      logout
    }),
    [state, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
