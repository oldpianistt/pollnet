import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useInfiniteQuery } from '@tanstack/react-query';
import { feedApi } from '../../api/endpoints';
import { Avatar, Badge, Button, Card, EmptyState, ErrorBanner, Skeleton, relativeTime } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { CursorPage, PollView } from '../../lib/types';
import { cn } from '../../lib/cn';

type Tab = 'discover' | 'following';

export function FeedPage() {
  const [tab, setTab] = useState<Tab>('discover');

  const query = useInfiniteQuery<CursorPage<PollView>>({
    queryKey: ['feed', tab],
    queryFn: ({ pageParam }) => feedApi.feed(tab, pageParam as string | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => last.nextCursor ?? undefined,
  });

  const items = query.data?.pages.flatMap((p) => p.items) ?? [];

  return (
    <div className="space-y-5">
      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-surface-border">
        <TabButton active={tab === 'discover'}  onClick={() => setTab('discover')}>Keşfet</TabButton>
        <TabButton active={tab === 'following'} onClick={() => setTab('following')}>Takip</TabButton>
        <Link to="/polls/new" className="ml-auto mb-2">
          <Button size="sm">
            <PlusIcon className="h-4 w-4" /> Anket oluştur
          </Button>
        </Link>
      </div>

      {query.isPending && (
        <div className="space-y-3">
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
        </div>
      )}

      {query.isError && <ErrorBanner message={errorMessage(query.error)} />}

      {!query.isPending && items.length === 0 && (
        <EmptyState
          icon={<SparkleIcon className="h-5 w-5" />}
          title={tab === 'following' ? 'Takip listen sessiz' : 'Henüz akış boş'}
          description={tab === 'following'
            ? 'Birilerini takip et — anketleri burada belirsin.'
            : 'İlk anketi sen oluşturarak başla.'}
          action={tab === 'discover'
            ? <Link to="/polls/new"><Button>Anket oluştur</Button></Link>
            : undefined}
        />
      )}

      <div className="space-y-3">
        {items.map((p) => <PollCard key={p.id} poll={p} />)}
      </div>

      {query.hasNextPage && (
        <div className="flex justify-center pt-2">
          <Button variant="outline" onClick={() => query.fetchNextPage()} disabled={query.isFetchingNextPage}>
            {query.isFetchingNextPage ? 'Yükleniyor…' : 'Daha fazla'}
          </Button>
        </div>
      )}
    </div>
  );
}

function TabButton({ active, onClick, children }: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        'relative px-4 py-3 text-sm font-medium transition-colors',
        active ? 'text-ink-900' : 'text-ink-500 hover:text-ink-700',
      )}
    >
      {children}
      {active && (
        <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-brand-600" />
      )}
    </button>
  );
}

function PollCard({ poll }: { poll: PollView }) {
  const author = poll.author.displayName ?? poll.author.username;
  return (
    <Link to={`/polls/${poll.id}`} className="block">
      <Card interactive className="hover:border-brand-200">
        <div className="flex items-start gap-3">
          <Avatar name={author} size="md" />
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline gap-1.5">
              <span className="font-medium text-ink-900">{author}</span>
              <span className="text-xs text-ink-500">@{poll.author.username}</span>
              <span className="text-xs text-ink-400">·</span>
              <span className="text-xs text-ink-500">{relativeTime(poll.createdAt)}</span>
            </div>

            <h2 className="mt-1.5 text-[17px] font-semibold leading-snug tracking-tightish text-ink-900">{poll.title}</h2>
            {poll.description && (
              <p className="mt-1 line-clamp-2 text-sm text-ink-500">{poll.description}</p>
            )}

            <div className="mt-3 flex flex-wrap items-center gap-2">
              <Badge tone="brand">{poll.questions.length} soru</Badge>
              {poll.viewerHasAnswered && (
                <Badge tone="success">
                  <CheckIcon className="h-3 w-3" /> Oyladın
                </Badge>
              )}
              <span className="ml-auto text-xs font-medium text-brand-600 group-hover:underline">
                Görüntüle →
              </span>
            </div>
          </div>
        </div>
      </Card>
    </Link>
  );
}

function CheckIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 5 5L20 7"/></svg>);
}
function PlusIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"><path d="M12 5v14M5 12h14"/></svg>);
}
function SparkleIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3v18M3 12h18M5 5l3 3M19 19l-3-3M19 5l-3 3M5 19l3-3"/></svg>);
}
