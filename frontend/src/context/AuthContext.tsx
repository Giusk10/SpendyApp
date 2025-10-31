import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

interface AuthState {
  token: string | null;
  username: string | null;
}

interface AuthContextValue extends AuthState {
  login: (token: string, username: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const storageKey = 'spendyapp_auth';

const readStorage = (): AuthState => {
  if (typeof window === 'undefined') {
    return { token: null, username: null };
  }
  const stored = window.localStorage.getItem(storageKey);
  if (!stored) {
    return { token: null, username: null };
  }
  try {
    return JSON.parse(stored) as AuthState;
  } catch (error) {
    console.warn('Failed to parse auth state', error);
    return { token: null, username: null };
  }
};

export const AuthProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const navigate = useNavigate();
  const [state, setState] = useState<AuthState>(readStorage);

  useEffect(() => {
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(storageKey, JSON.stringify(state));
    }
  }, [state]);

  const login = (token: string, username: string) => {
    setState({ token, username });
    navigate('/', { replace: true });
  };

  const logout = () => {
    setState({ token: null, username: null });
    navigate('/', { replace: true });
  };

  const value = useMemo(() => ({ ...state, login, logout }), [state]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
