import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { invitationApi } from '../../api/endpoints';
import { Button, Card, ErrorBanner, Spinner } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { InvitationView, QuotaView } from '../../lib/types';

export function InvitationsPage() {
  const qc = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [copiedToken, setCopiedToken] = useState<string | null>(null);

  const quota = useQuery<QuotaView>({ queryKey: ['quota'], queryFn: invitationApi.quota });
  const list = useQuery<InvitationView[]>({ queryKey: ['invitations'], queryFn: invitationApi.myInvitations });

  const create = useMutation({
    mutationFn: invitationApi.create,
    onSuccess: () => {
      setError(null);
      qc.invalidateQueries({ queryKey: ['invitations'] });
      qc.invalidateQueries({ queryKey: ['quota'] });
    },
    onError: (err) => setError(errorMessage(err)),
  });

  function copyLink(token: string) {
    const url = `${window.location.origin}/register?invite=${encodeURIComponent(token)}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopiedToken(token);
      setTimeout(() => setCopiedToken((t) => (t === token ? null : t)), 1500);
    });
  }

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold">Davetler</h1>
            {quota.data && (
              <p className="mt-1 text-sm text-slate-600">
                Kalan: <strong>{quota.data.remaining}</strong> / {quota.data.monthlyAllowance}
                <span className="text-slate-400"> · Bu sayı her ayın 1'inde sıfırlanır.</span>
              </p>
            )}
          </div>
          <Button
            onClick={() => create.mutate()}
            disabled={create.isPending || (quota.data?.remaining ?? 0) === 0}
          >
            {create.isPending ? 'Oluşturuluyor…' : 'Yeni davet üret'}
          </Button>
        </div>
        {error && <div className="mt-3"><ErrorBanner message={error} /></div>}
      </Card>

      {list.isPending && <div className="flex justify-center"><Spinner /></div>}

      {list.data && list.data.length === 0 && (
        <Card className="text-center text-sm text-slate-500">
          Henüz kullanılmamış davetin yok.
        </Card>
      )}

      {list.data?.map((inv) => (
        <Card key={inv.id} className="space-y-2">
          <div className="flex items-center justify-between gap-3">
            <code className="break-all text-xs text-slate-600">{inv.token}</code>
            <Button variant="outline" className="h-8 px-3 text-xs" onClick={() => copyLink(inv.token)}>
              {copiedToken === inv.token ? 'Kopyalandı ✓' : 'Linki kopyala'}
            </Button>
          </div>
          <div className="text-xs text-slate-500">
            Oluşturuldu: {new Date(inv.createdAt).toLocaleString('tr-TR')}
            {' · '}
            Son geçerlilik: {new Date(inv.expiresAt).toLocaleString('tr-TR')}
          </div>
        </Card>
      ))}
    </div>
  );
}
