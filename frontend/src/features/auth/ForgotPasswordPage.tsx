import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../../api/endpoints';
import { Button, Card, ErrorBanner, Input, Label } from '../../components/ui';
import { errorMessage } from '../../lib/errors';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await authApi.forgotPassword(email.trim());
      setSubmitted(true);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto max-w-md space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Şifremi unuttum</h1>
        <p className="text-sm text-slate-600">E-postana sıfırlama bağlantısı yollayalım</p>
      </div>
      <Card>
        {submitted ? (
          <p className="text-sm text-slate-700">
            Eğer bu e-posta sistemde kayıtlıysa, sıfırlama bağlantısı yollandı.
            Birkaç dakika içinde gelmezse spam klasörünü kontrol et.
          </p>
        ) : (
          <form className="space-y-4" onSubmit={handleSubmit}>
            <ErrorBanner message={error} />
            <div className="space-y-1">
              <Label htmlFor="email">E-posta</Label>
              <Input id="email" type="email" autoComplete="email"
                     value={email} onChange={(e) => setEmail(e.target.value)} required />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Gönderiliyor…' : 'Bağlantı yolla'}
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
