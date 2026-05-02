import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { messagingApi, userApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Avatar, Badge, Button, Card, EmptyState, ErrorBanner, Skeleton, isOnline, relativeTime } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { PollView, PublicProfile } from '../../lib/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
function fullAvatarUrl(u: string | null | undefined): string | null {
  if (!u) return null;
  return u.startsWith('http') ? u : `${baseURL}${u}`;
}

export function ProfilePage() {
  const { username } = useParams<{ username: string }>();
  const me = useAuth((s) => s.user);
  const navigate = useNavigate();
  const qc = useQueryClient();

  const profile = useQuery<PublicProfile>({
    queryKey: ['profile', username],
    queryFn: () => userApi.profile(username!),
    enabled: !!username,
  });

  const polls = useQuery<PollView[]>({
    queryKey: ['user-polls', username],
    queryFn: () => userApi.userPolls(username!),
    enabled: !!username,
  });

  const followMutation = useMutation({
    mutationFn: (next: boolean) =>
      next ? userApi.follow(username!) : userApi.unfollow(username!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['profile', username] }),
  });

  const dmMutation = useMutation({
    mutationFn: () => messagingApi.open(username!),
    onSuccess: (convo) => navigate(`/messages/${convo.id}`),
  });

  if (profile.isPending) return <ProfileSkeleton />;
  if (profile.isError) return <ErrorBanner message={errorMessage(profile.error)} />;

  const p = profile.data!;
  const isMe = me?.id === p.id;
  const display = p.displayName ?? p.username;

  return (
    <div className="space-y-6">
      <Card padded={false} className="overflow-hidden">
        <div className="h-28 gradient-brand" />

        <div className="px-6 pb-6">
          <div className="-mt-12 flex flex-wrap items-end justify-between gap-3">
            <Avatar
              name={display}
              src={fullAvatarUrl(p.avatarUrl)}
              online={isOnline(p.lastSeenAt)}
              size="xl"
              className="ring-4 ring-surface-raised"
            />
            <div className="flex items-center gap-2 self-end">
              {isMe ? (
                <Link to="/settings/profile">
                  <Button variant="outline" size="md">
                    <PencilIcon className="h-4 w-4" /> Profili düzenle
                  </Button>
                </Link>
              ) : me ? (
                <>
                  <Button
                    variant={p.viewerFollows ? 'outline' : 'primary'}
                    onClick={() => followMutation.mutate(!p.viewerFollows)}
                    disabled={followMutation.isPending}
                  >
                    {p.viewerFollows ? (<><CheckIcon className="h-4 w-4" /> Takip ediliyor</>) : 'Takip et'}
                  </Button>
                  <Button
                    variant="outline"
                    size="md"
                    onClick={() => dmMutation.mutate()}
                    disabled={dmMutation.isPending}
                  >
                    <MailIcon className="h-4 w-4" /> Mesaj
                  </Button>
                </>
              ) : null}
            </div>
          </div>

          <div className="mt-3">
            <h1 className="text-2xl font-bold tracking-tightish text-ink-900">{display}</h1>
            <div className="mt-0.5 flex flex-wrap items-center gap-2 text-sm text-ink-500">
              <span>@{p.username}</span>
              <span className="text-ink-400">·</span>
              <span>Üyelik: {new Date(p.createdAt).toLocaleDateString('tr-TR', { month: 'long', year: 'numeric' })}</span>
              {isOnline(p.lastSeenAt) ? (
                <Badge tone="success">çevrimiçi</Badge>
              ) : p.lastSeenAt && !isMe ? (
                <span className="text-xs">son görülme {relativeTime(p.lastSeenAt)}</span>
              ) : null}
            </div>

            {p.bio && (
              <p className="mt-3 text-[15px] leading-relaxed text-ink-700 whitespace-pre-wrap">{p.bio}</p>
            )}

            <div className="mt-5 grid grid-cols-3 gap-2 sm:max-w-md">
              <StatPill label="anket" value={polls.data?.length ?? 0} />
              <StatPill label="takipçi" value={p.followerCount} />
              <StatPill label="takip" value={p.followingCount} />
            </div>
          </div>
        </div>
      </Card>

      <section className="space-y-3">
        <div className="flex items-baseline justify-between px-1">
          <h2 className="text-base font-semibold tracking-tightish text-ink-900">Anketleri</h2>
          {(polls.data?.length ?? 0) > 0 && (
            <span className="text-xs text-ink-500">{polls.data!.length} anket</span>
          )}
        </div>

        {polls.isPending && (
          <div className="space-y-3">
            <Skeleton className="h-24" />
            <Skeleton className="h-24" />
          </div>
        )}

        {polls.data && polls.data.length === 0 && (
          <EmptyState
            icon={<ChartIcon className="h-5 w-5" />}
            title="Henüz anket yok"
            description={isMe
              ? 'İlk anketini oluşturarak başla — herkes katılabilsin.'
              : `${display} henüz anket paylaşmamış.`}
            action={isMe ? <Link to="/polls/new"><Button>Anket oluştur</Button></Link> : undefined}
          />
        )}

        <div className="space-y-3">
          {polls.data?.map((poll) => (
            <Link key={poll.id} to={`/polls/${poll.id}`} className="block">
              <Card interactive>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <h3 className="truncate font-semibold text-ink-900">{poll.title}</h3>
                    {poll.description && (
                      <p className="mt-1 line-clamp-2 text-sm text-ink-500">{poll.description}</p>
                    )}
                    <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-ink-500">
                      <Badge tone="neutral">{poll.questions.length} soru</Badge>
                      {poll.viewerHasAnswered && <Badge tone="success">Oyladın</Badge>}
                      <span>· {relativeTime(poll.createdAt)}</span>
                    </div>
                  </div>
                  <ChevronRight className="h-5 w-5 flex-shrink-0 text-ink-400" />
                </div>
              </Card>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}

function StatPill({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border border-surface-border bg-surface-subtle/50 px-3 py-2">
      <div className="text-lg font-semibold tracking-tightish text-ink-900">{value}</div>
      <div className="text-xs text-ink-500">{label}</div>
    </div>
  );
}

function ProfileSkeleton() {
  return (
    <div className="space-y-6">
      <Card padded={false} className="overflow-hidden">
        <div className="h-28 bg-surface-subtle" />
        <div className="px-6 pb-6">
          <div className="-mt-12 flex items-end justify-between">
            <Skeleton className="h-24 w-24 rounded-full ring-4 ring-surface-raised" />
            <Skeleton className="h-10 w-28 rounded" />
          </div>
          <div className="mt-3 space-y-2">
            <Skeleton className="h-6 w-40" />
            <Skeleton className="h-4 w-56" />
            <Skeleton className="h-4 w-72" />
            <div className="mt-5 grid grid-cols-3 gap-2 sm:max-w-md">
              <Skeleton className="h-14" /><Skeleton className="h-14" /><Skeleton className="h-14" />
            </div>
          </div>
        </div>
      </Card>
      <Skeleton className="h-24" /><Skeleton className="h-24" />
    </div>
  );
}

function PencilIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 1 1 3 3L7 19l-4 1 1-4Z"/></svg>);
}
function CheckIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 5 5L20 7"/></svg>);
}
function MailIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>);
}
function ChartIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 3v18h18"/><rect x="7" y="13" width="3" height="6"/><rect x="12" y="9" width="3" height="10"/><rect x="17" y="5" width="3" height="14"/></svg>);
}
function ChevronRight({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m9 18 6-6-6-6"/></svg>);
}
