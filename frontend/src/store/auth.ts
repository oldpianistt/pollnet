import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse } from '../lib/types';

interface AuthState {
  user: AuthResponse['user'] | null;
  accessToken: string | null;
  refreshToken: string | null;
  setAuth: (resp: AuthResponse) => void;
  setTokens: (access: string, refresh: string) => void;
  clear: () => void;
}

export const useAuth = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      setAuth: (resp) =>
        set({ user: resp.user, accessToken: resp.accessToken, refreshToken: resp.refreshToken }),
      setTokens: (access, refresh) => set({ accessToken: access, refreshToken: refresh }),
      clear: () => set({ user: null, accessToken: null, refreshToken: null }),
    }),
    { name: 'pollnet-auth' },
  ),
);
