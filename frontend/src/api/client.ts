import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';
import { useAuth } from '../store/auth';
import type { AuthResponse } from '../lib/types';

// Empty string is a valid value (same-origin: API served via reverse proxy).
// Only fall back when the env var was *not set* at build time.
const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';

export const api = axios.create({ baseURL });

api.interceptors.request.use((config) => {
  const token = useAuth.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Refresh-on-401: queue concurrent failures so we only refresh once.
let refreshPromise: Promise<string | null> | null = null;

async function refreshTokens(): Promise<string | null> {
  const refreshToken = useAuth.getState().refreshToken;
  if (!refreshToken) return null;
  try {
    const resp = await axios.post<AuthResponse>(
      `${baseURL}/api/auth/refresh`,
      { refreshToken },
      { headers: { 'Content-Type': 'application/json' } },
    );
    useAuth.getState().setTokens(resp.data.accessToken, resp.data.refreshToken);
    return resp.data.accessToken;
  } catch {
    useAuth.getState().clear();
    return null;
  }
}

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    const status = error.response?.status;
    const isRefreshCall = original?.url?.endsWith('/api/auth/refresh');

    if (status !== 401 || original?._retry || isRefreshCall) {
      return Promise.reject(error);
    }

    original._retry = true;
    refreshPromise ??= refreshTokens().finally(() => {
      refreshPromise = null;
    });

    const newToken = await refreshPromise;
    if (!newToken) {
      return Promise.reject(error);
    }
    original.headers = original.headers ?? {};
    (original.headers as Record<string, string>).Authorization = `Bearer ${newToken}`;
    return api.request(original as AxiosRequestConfig);
  },
);
