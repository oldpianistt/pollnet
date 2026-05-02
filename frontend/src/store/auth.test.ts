import { beforeEach, describe, expect, it } from 'vitest';
import { useAuth } from './auth';

describe('auth store', () => {
  beforeEach(() => {
    // Persisted middleware writes to localStorage; clear it between tests.
    localStorage.clear();
    useAuth.getState().clear();
  });

  it('starts empty', () => {
    const s = useAuth.getState();
    expect(s.user).toBeNull();
    expect(s.accessToken).toBeNull();
    expect(s.refreshToken).toBeNull();
  });

  it('setAuth populates user + tokens', () => {
    useAuth.getState().setAuth({
      accessToken: 'a',
      refreshToken: 'r',
      accessTtlSeconds: 900,
      user: { id: 'u1', username: 'alice', email: 'a@x.io', displayName: 'Alice' },
    });
    const s = useAuth.getState();
    expect(s.accessToken).toBe('a');
    expect(s.refreshToken).toBe('r');
    expect(s.user?.username).toBe('alice');
  });

  it('setTokens rotates without losing user', () => {
    useAuth.getState().setAuth({
      accessToken: 'a',
      refreshToken: 'r',
      accessTtlSeconds: 900,
      user: { id: 'u1', username: 'alice', email: 'a@x.io', displayName: null },
    });
    useAuth.getState().setTokens('a2', 'r2');
    const s = useAuth.getState();
    expect(s.accessToken).toBe('a2');
    expect(s.refreshToken).toBe('r2');
    expect(s.user?.username).toBe('alice'); // user preserved
  });

  it('clear wipes everything', () => {
    useAuth.getState().setAuth({
      accessToken: 'a',
      refreshToken: 'r',
      accessTtlSeconds: 900,
      user: { id: 'u1', username: 'alice', email: 'a@x.io', displayName: null },
    });
    useAuth.getState().clear();
    const s = useAuth.getState();
    expect(s.user).toBeNull();
    expect(s.accessToken).toBeNull();
    expect(s.refreshToken).toBeNull();
  });
});
