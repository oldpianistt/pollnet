import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { userApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Avatar, Button, Card, ErrorBanner, Input, Label, Spinner, Textarea } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { MeView } from '../../lib/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';

function fullAvatarUrl(url: string | null | undefined): string | null {
  if (!url) return null;
  return url.startsWith('http') ? url : `${baseURL}${url}`;
}

export function EditProfilePage() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const setAuthUser = useAuth.setState;

  const meQ = useQuery<MeView>({ queryKey: ['me'], queryFn: userApi.me });

  const [displayName, setDisplayName] = useState('');
  const [bio, setBio]                 = useState('');
  const [error, setError]             = useState<string | null>(null);

  // Initialise form once data lands.
  useEffect(() => {
    if (meQ.data) {
      setDisplayName(meQ.data.displayName ?? '');
      setBio(meQ.data.bio ?? '');
    }
  }, [meQ.data]);

  const updateText = useMutation({
    mutationFn: () =>
      userApi.updateMe({
        displayName: displayName.trim() || undefined,
        bio:         bio.trim()         || undefined,
      }),
    onSuccess: (me) => {
      qc.setQueryData(['me'], me);
      qc.invalidateQueries({ queryKey: ['profile', me.username] });
      // Sync the persisted auth user (so navbar updates without re-login).
      setAuthUser((s) => ({
        ...s,
        user: s.user ? { ...s.user, displayName: me.displayName, avatarUrl: me.avatarUrl } : s.user,
      }));
      navigate(`/u/${me.username}`);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  if (meQ.isPending) return <div className="flex justify-center py-16"><Spinner /></div>;
  if (meQ.isError)   return <ErrorBanner message={errorMessage(meQ.error)} />;
  const me = meQ.data!;

  return (
    <div className="space-y-5">
      <header className="flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tightish text-ink-900">Profili düzenle</h1>
        <Button variant="ghost" onClick={() => navigate(-1)}>İptal</Button>
      </header>

      <AvatarSection me={me} />

      <Card>
        <form className="space-y-4" onSubmit={(e: FormEvent) => { e.preventDefault(); updateText.mutate(); }}>
          <ErrorBanner message={error} />
          <div className="space-y-1.5">
            <Label htmlFor="display">İsim</Label>
            <Input id="display" value={displayName} onChange={(e) => setDisplayName(e.target.value)} maxLength={64} placeholder="Profilinde görünecek ad" />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="bio">Bio</Label>
            <Textarea id="bio" rows={4} maxLength={1000} value={bio} onChange={(e) => setBio(e.target.value)} placeholder="Kendinden bahset…" />
            <div className="text-right text-xs text-ink-400">{bio.length}/1000</div>
          </div>
          <div className="space-y-1.5">
            <Label>E-posta</Label>
            <Input value={me.email} disabled />
            <p className="text-xs text-ink-500">E-posta değişikliği şimdilik kapalı.</p>
          </div>
          <div className="pt-2 flex justify-end">
            <Button type="submit" disabled={updateText.isPending}>
              {updateText.isPending ? 'Kaydediliyor…' : 'Kaydet'}
            </Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

function AvatarSection({ me }: { me: MeView }) {
  const qc = useQueryClient();
  const setAuthUser = useAuth.setState;
  const fileRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const upload = useMutation({
    mutationFn: (f: File) => userApi.uploadAvatar(f),
    onSuccess: (updated) => {
      qc.setQueryData(['me'], updated);
      qc.invalidateQueries({ queryKey: ['profile', updated.username] });
      setAuthUser((s) => ({
        ...s,
        user: s.user ? { ...s.user, avatarUrl: updated.avatarUrl } : s.user,
      }));
      setError(null);
    },
    onError: (e) => setError(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: () => userApi.removeAvatar(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['me'] });
      qc.invalidateQueries({ queryKey: ['profile', me.username] });
      setAuthUser((s) => ({
        ...s,
        user: s.user ? { ...s.user, avatarUrl: null } : s.user,
      }));
    },
  });

  function onPick(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (f) upload.mutate(f);
    e.target.value = '';
  }

  return (
    <Card>
      <div className="flex items-center gap-4">
        <Avatar
          name={me.displayName ?? me.username}
          src={fullAvatarUrl(me.avatarUrl)}
          size="xl"
        />
        <div className="flex-1">
          <p className="font-medium text-ink-900">Profil fotoğrafı</p>
          <p className="text-xs text-ink-500">JPG, PNG, GIF veya WebP — en fazla 2 MB</p>

          <div className="mt-3 flex flex-wrap gap-2">
            <Button onClick={() => fileRef.current?.click()} disabled={upload.isPending}>
              {upload.isPending ? 'Yükleniyor…' : 'Fotoğraf yükle'}
            </Button>
            {me.avatarUrl && (
              <Button variant="outline" onClick={() => remove.mutate()} disabled={remove.isPending}>
                Kaldır
              </Button>
            )}
          </div>
          <input ref={fileRef} type="file" accept="image/*" onChange={onPick} className="hidden" />
          {error && <div className="mt-2 text-sm text-red-600">{error}</div>}
        </div>
      </div>
    </Card>
  );
}
