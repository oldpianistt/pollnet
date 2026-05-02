import * as Sentry from '@sentry/react';

/**
 * Initialise Sentry only when a DSN is configured at build time. When VITE_SENTRY_DSN
 * is empty/undefined, every Sentry call becomes a no-op — no network traffic, no
 * runtime cost beyond an unused module import.
 */
export function initSentry(): void {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment: import.meta.env.VITE_SENTRY_ENVIRONMENT ?? import.meta.env.MODE,
    release:     import.meta.env.VITE_SENTRY_RELEASE     ?? '0.1.0',
    tracesSampleRate:        Number(import.meta.env.VITE_SENTRY_TRACES_SAMPLE_RATE ?? '0'),
    replaysSessionSampleRate: Number(import.meta.env.VITE_SENTRY_REPLAYS_SAMPLE_RATE ?? '0'),
    replaysOnErrorSampleRate: 1.0,
  });
}
