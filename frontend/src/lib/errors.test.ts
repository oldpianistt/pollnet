import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { errorMessage } from './errors';

describe('errorMessage', () => {
  it('extracts ApiError.message from an axios response', () => {
    const err = new AxiosError('Request failed');
    err.response = {
      data: { message: 'Username taken', code: 'USERNAME_TAKEN', status: 409 },
      status: 409, statusText: 'Conflict', headers: {}, config: {} as never,
    };
    expect(errorMessage(err)).toBe('Username taken');
  });

  it('falls back to axios .message when no body', () => {
    const err = new AxiosError('Network error');
    expect(errorMessage(err)).toBe('Network error');
  });

  it('falls back to error.message for plain Error', () => {
    expect(errorMessage(new Error('boom'))).toBe('boom');
  });

  it('returns the fallback for unknown shapes', () => {
    expect(errorMessage('weird-string', 'default')).toBe('default');
  });
});
