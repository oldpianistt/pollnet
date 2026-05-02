import React from 'react';
import ReactDOM from 'react-dom/client';
import * as Sentry from '@sentry/react';
import App from './App';
import { initSentry } from './lib/sentry';
import './index.css';

initSentry();

const Root = (
  // ErrorBoundary is a no-op fallback when Sentry is disabled (DSN unset).
  // When enabled, captures rendering errors and surfaces a user-friendly fallback.
  <Sentry.ErrorBoundary fallback={ErrorFallback} showDialog={false}>
    <App />
  </Sentry.ErrorBoundary>
);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>{Root}</React.StrictMode>,
);

function ErrorFallback() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-3 p-6 text-center text-slate-700">
      <h1 className="text-xl font-semibold">Bir şeyler ters gitti</h1>
      <p className="text-sm">Sayfayı yenilemek genellikle işe yarar.</p>
      <button onClick={() => location.reload()}
              className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white">
        Sayfayı yenile
      </button>
    </div>
  );
}
