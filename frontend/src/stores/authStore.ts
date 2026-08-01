import { create } from 'zustand';
import type { CurrentUser, TokenResponse } from '../types/api';

const TOKEN_KEY = 'rag_access_token';
const REFRESH_KEY = 'rag_refresh_token';

const read = (key: string) => localStorage.getItem(key) ?? sessionStorage.getItem(key);

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: CurrentUser | null;
  setSession: (v: TokenResponse, remember?: boolean) => void;
  setUser: (u: CurrentUser) => void;
  clear: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: read(TOKEN_KEY),
  refreshToken: read(REFRESH_KEY),
  user: null,
  setSession: (v, remember = true) => {
    const storage = remember ? localStorage : sessionStorage;
    storage.setItem(TOKEN_KEY, v.accessToken);
    storage.setItem(REFRESH_KEY, v.refreshToken);
    // 同时清理另一种存储，避免残留
    const other = remember ? sessionStorage : localStorage;
    other.removeItem(TOKEN_KEY);
    other.removeItem(REFRESH_KEY);
    set({ accessToken: v.accessToken, refreshToken: v.refreshToken });
  },
  setUser: (user) => set({ user }),
  clear: () => {
    [localStorage, sessionStorage].forEach((s) => {
      s.removeItem(TOKEN_KEY);
      s.removeItem(REFRESH_KEY);
    });
    set({ accessToken: null, refreshToken: null, user: null });
  },
}));
