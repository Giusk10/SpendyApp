import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import { setAuthToken } from '../api/httpClient';

interface AuthContextState {
  token: string | null;
  username: string | null;
  isAuthenticated: boolean;
  login: (token: string, username?: string | null) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextState | undefined>(undefined);

const TOKEN_STORAGE_KEY = 'spendy.auth.token';
const USERNAME_STORAGE_KEY = 'spendy.auth.username';

type Props = {
  children: React.ReactNode;
};

export const AuthProvider: React.FC<Props> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [username, setUsername] = useState<string | null>(() => localStorage.getItem(USERNAME_STORAGE_KEY));

  useEffect(() => {
    if (token) {
      setAuthToken(token);
      localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } else {
      setAuthToken(undefined);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }
  }, [token]);

  useEffect(() => {
    if (username) {
      localStorage.setItem(USERNAME_STORAGE_KEY, username);
    } else {
      localStorage.removeItem(USERNAME_STORAGE_KEY);
    }
  }, [username]);

  const login = useCallback((newToken: string, newUsername?: string | null) => {
    setToken(newToken);
    if (newUsername) {
      setUsername(newUsername);
    }
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUsername(null);
  }, []);

  const value = useMemo(() => ({
    token,
    username,
    isAuthenticated: Boolean(token),
    login,
    logout
  }), [token, username, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
