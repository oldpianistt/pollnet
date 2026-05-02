import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Button, Card, ErrorBanner, Input, Label } from '../../components/ui';
import { errorMessage } from '../../lib/errors';

export function LoginPage() {
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const setAuth = useAuth((s) => s.setAuth);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const resp = await authApi.login({ usernameOrEmail, password });
      setAuth(resp);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Giriş yap</h1>
        <p className="text-sm text-slate-600">Hesabınla devam et</p>
      </div>
      <Card>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <ErrorBanner message={error} />
          <div className="space-y-1">
            <Label htmlFor="login-id">Kullanıcı adı veya e-posta</Label>
            <Input
              id="login-id"
              autoComplete="username"
              value={usernameOrEmail}
              onChange={(e) => setUsernameOrEmail(e.target.value)}
              required
            />
          </div>
          <div className="space-y-1">
            <Label htmlFor="login-pw">Şifre</Label>
            <Input
              id="login-pw"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Giriş yapılıyor…' : 'Giriş yap'}
          </Button>
          <div className="text-center text-sm">
            <Link className="text-slate-600 hover:underline" to="/forgot-password">
              Şifremi unuttum
            </Link>
          </div>
        </form>
      </Card>
      <p className="text-center text-sm text-slate-600">
        Davet kodun var mı?{' '}
        <Link className="font-medium text-slate-900 hover:underline" to="/register">
          Kayıt ol
        </Link>
      </p>
    </div>
  );
}
