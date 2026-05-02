import { AxiosError } from 'axios';
import type { ApiError } from './types';

export function errorMessage(err: unknown, fallback = 'Bir hata oluştu'): string {
  if (err instanceof AxiosError) {
    const data = err.response?.data as Partial<ApiError> | undefined;
    if (data?.message) return data.message;
    if (err.message) return err.message;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}
