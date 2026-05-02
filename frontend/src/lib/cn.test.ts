import { describe, expect, it } from 'vitest';
import { cn } from './cn';

describe('cn', () => {
  it('joins classnames', () => {
    expect(cn('a', 'b')).toBe('a b');
  });

  it('respects conditional values', () => {
    expect(cn('a', false && 'b', 'c')).toBe('a c');
  });

  it('merges conflicting tailwind utilities (later wins)', () => {
    // tailwind-merge collapses px-2 + px-4 → px-4
    expect(cn('px-2 py-1', 'px-4')).toBe('py-1 px-4');
  });
});
