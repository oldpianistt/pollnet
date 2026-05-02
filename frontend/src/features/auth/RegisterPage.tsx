import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Button, Card, ErrorBanner, Input, Label } from '../../components/ui';
import { errorMessage } from '../../lib/errors';

export function RegisterPage() {
  const [searchParams] = useSearchParams();
  const [inviteToken, setInviteToken] = useState(searchParams.get('invite') ?? '');
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const setAuth = useAuth((s) => s.setAuth);
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const resp = await authApi.register({
        inviteToken: inviteToken.trim(),
        username: username.trim(),
        email: email.trim(),
        password,
        displayName: displayName.trim() || undefined,
      });
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
        <h1 className="text-2xl font-bold">Kayıt ol</h1>
        <p className="text-sm text-slate-600">Davet bazlı bir ağız — bir davet kodu gerek</p>
      </div>
      <Card>
        <form className="space-y-4" onSubmit={handleSubmit}>
          <ErrorBanner message={error} />
          <div className="space-y-1">
            <Label htmlFor="invite">Davet kodu</Label>
            <Input id="invite" value={inviteToken} onChange={(e) => setInviteToken(e.target.value)} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1">
              <Label htmlFor="username">Kullanıcı adı</Label>
              <Input id="username" value={username} onChange={(e) => setUsername(e.target.value)} required minLength={3} maxLength={32} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="display">İsim</Label>
              <Input id="display" value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={64} />
            </div>
          </div>
          <div className="space-y-1">
            <Label htmlFor="email">E-posta</Label>
            <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>
          <div className="space-y-1">
            <Label htmlFor="pw">Şifre (en az 8 karakter)</Label>
            <Input id="pw" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Kayıt yapılıyor…' : 'Kayıt ol'}
          </Button>
        </form>
      </Card>
      <p className="text-center text-sm text-slate-600">
        Zaten üye misin?{' '}
        <Link className="font-medium text-slate-900 hover:underline" to="/login">
          Giriş yap
        </Link>
      </p>
    </div>
  );
}
