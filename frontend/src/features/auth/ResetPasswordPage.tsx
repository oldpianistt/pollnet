import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { Button, Card, ErrorBanner, Input, Label } from '../../components/ui';
import { errorMessage } from '../../lib/errors';

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const initialToken = params.get('token') ?? '';
  const [token, setToken] = useState(initialToken);
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password !== confirm) {
      setError('Şifreler eşleşmiyor');
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword(token.trim(), password);
      setDone(true);
      setTimeout(() => navigate('/login'), 1500);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Şifreyi sıfırla</h1>
      </div>
      <Card>
        {done ? (
          <p className="text-sm text-slate-700">
            Şifren güncellendi. Giriş sayfasına yönlendiriliyorsun…
          </p>
        ) : (
          <form className="space-y-4" onSubmit={handleSubmit}>
            <ErrorBanner message={error} />
            {!initialToken && (
              <div className="space-y-1">
                <Label htmlFor="token">Token</Label>
                <Input id="token" value={token} onChange={(e) => setToken(e.target.value)} required />
              </div>
            )}
            <div className="space-y-1">
              <Label htmlFor="pw">Yeni şifre (en az 8 karakter)</Label>
              <Input id="pw" type="password" autoComplete="new-password"
                     value={password} onChange={(e) => setPassword(e.target.value)}
                     required minLength={8} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="confirm">Tekrar yaz</Label>
              <Input id="confirm" type="password" autoComplete="new-password"
                     value={confirm} onChange={(e) => setConfirm(e.target.value)}
                     required minLength={8} />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Güncelleniyor…' : 'Şifreyi değiştir'}
            </Button>
          </form>
        )}
      </Card>
      <p className="text-center text-sm text-slate-600">
        <Link className="font-medium text-slate-900 hover:underline" to="/login">Girişe dön</Link>
      </p>
    </div>
  );
}
