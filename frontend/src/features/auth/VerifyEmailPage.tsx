import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { Card, Spinner } from '../../components/ui';
import { errorMessage } from '../../lib/errors';

type State =
  | { kind: 'loading' }
  | { kind: 'success' }
  | { kind: 'error'; message: string };

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [state, setState] = useState<State>({ kind: 'loading' });

  // Don't double-fire in StrictMode dev — verify is one-shot, second call would fail.
  const fired = useRef(false);

  useEffect(() => {
    if (fired.current) return;
    fired.current = true;
    if (!token) {
      setState({ kind: 'error', message: 'Token eksik.' });
      return;
    }
    authApi.verifyEmail(token)
      .then(() => setState({ kind: 'success' }))
      .catch((err) => setState({ kind: 'error', message: errorMessage(err) }));
  }, [token]);

  return (
    <div className="mx-auto max-w-md space-y-6">
      <Card className="text-center">
        {state.kind === 'loading' && (
          <div className="flex flex-col items-center gap-2">
            <Spinner />
            <p className="text-sm text-slate-600">E-posta doğrulanıyor…</p>
          </div>
        )}
        {state.kind === 'success' && (
          <>
            <h1 className="text-xl font-semibold">E-posta doğrulandı ✓</h1>
            <p className="mt-2 text-sm text-slate-600">PollNet'e hoş geldin.</p>
          </>
        )}
        {state.kind === 'error' && (
          <>
            <h1 className="text-xl font-semibold text-red-700">Doğrulanamadı</h1>
            <p className="mt-2 text-sm text-slate-600">{state.message}</p>
          </>
        )}
      </Card>
      <p className="text-center text-sm text-slate-600">
        <Link className="font-medium text-slate-900 hover:underline" to="/">Ana sayfaya dön</Link>
      </p>
    </div>
  );
}
