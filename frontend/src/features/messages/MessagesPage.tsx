import { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { messagingApi } from '../../api/endpoints';
import { Avatar, Badge, Card, EmptyState, Spinner, isOnline, relativeTime } from '../../components/ui';
import type { ConversationView } from '../../lib/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
const fullUrl = (u: string | null | undefined) => (!u ? null : (u.startsWith('http') ? u : `${baseURL}${u}`));

const POLL_INTERVAL_MS = 8_000;

/** Inbox + active thread, two-column on desktop, stacked on mobile. */
export function MessagesPage() {
  const { id } = useParams<{ id?: string }>();
  const qc = useQueryClient();

  const list = useQuery<ConversationView[]>({
    queryKey: ['conversations'],
    queryFn:  messagingApi.conversations,
    refetchInterval: POLL_INTERVAL_MS,
  });

  // Whenever messages change in the open thread, the inbox preview should refresh too.
  useEffect(() => {
    if (id) qc.invalidateQueries({ queryKey: ['conversations'] });
  }, [id, qc]);

  return (
    <div className="grid grid-cols-1 md:grid-cols-[300px_1fr] gap-4 -mx-2 md:-mx-0">
      <aside className={['md:block', id ? 'hidden' : 'block'].join(' ')}>
        <Card padded={false}>
          <header className="flex items-center justify-between border-b border-surface-border px-4 py-3">
            <h2 className="font-semibold tracking-tightish text-ink-900">Mesajlar</h2>
            <Badge tone="info">deneysel</Badge>
          </header>

          {list.isPending && <div className="flex justify-center py-8"><Spinner /></div>}

          {!list.isPending && (list.data?.length ?? 0) === 0 && (
            <div className="px-4 py-8">
              <EmptyState
                icon={<MailIcon className="h-5 w-5" />}
                title="Henüz konuşma yok"
                description="Bir profile gidip 'Mesaj' butonuna basarak başlayabilirsin."
              />
            </div>
          )}

          <ul>
            {list.data?.map((c) => {
              const display = c.peer.displayName ?? c.peer.username;
              const active = id === c.id;
              return (
                <li key={c.id}>
                  <Link
                    to={`/messages/${c.id}`}
                    className={[
                      'flex items-start gap-3 px-4 py-3 transition-colors',
                      active ? 'bg-brand-50' : 'hover:bg-surface-subtle',
                    ].join(' ')}
                  >
                    <Avatar
                      name={display}
                      src={fullUrl(c.peer.avatarUrl)}
                      online={isOnline(c.peer.lastSeenAt)}
                      size="md"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex items-baseline justify-between gap-2">
                        <span className={['truncate font-medium', active ? 'text-brand-700' : 'text-ink-900'].join(' ')}>
                          {display}
                        </span>
                        <span className="shrink-0 text-[11px] text-ink-400">{relativeTime(c.lastMessageAt)}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <p className="truncate text-sm text-ink-500">
                          {c.lastMessagePreview ?? <span className="italic text-ink-400">Yeni konuşma</span>}
                        </p>
                        {c.unreadCount > 0 && (
                          <span className="ml-auto inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full gradient-brand px-1 text-[10px] font-semibold text-white">
                            {c.unreadCount > 99 ? '99+' : c.unreadCount}
                          </span>
                        )}
                      </div>
                    </div>
                  </Link>
                </li>
              );
            })}
          </ul>
        </Card>
      </aside>

      <section className={['md:block min-h-[60vh]', id ? 'block' : 'hidden md:block'].join(' ')}>
        {id ? <Thread conversationId={id} /> : <NoThreadHint />}
      </section>
    </div>
  );
}

function NoThreadHint() {
  return (
    <Card className="h-full grid place-items-center text-center min-h-[50vh]">
      <div className="space-y-2">
        <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-brand-50 text-brand-600">
          <MailIcon className="h-5 w-5" />
        </div>
        <p className="font-semibold text-ink-900">Bir konuşma seç</p>
        <p className="text-sm text-ink-500">Soldan birini seç ya da bir profilden mesaj gönder.</p>
      </div>
    </Card>
  );
}

/* ───────────── Thread ───────────── */

import { useRef, useState, type FormEvent } from 'react';
import { useInfiniteQuery, useMutation } from '@tanstack/react-query';
import { useAuth } from '../../store/auth';
import { Button, Textarea } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { CursorPage, MessageView } from '../../lib/types';

const READ_INTERVAL_MS    = 12_000;
const REFRESH_INTERVAL_MS = 6_000;

function Thread({ conversationId }: { conversationId: string }) {
  const me = useAuth((s) => s.user);
  const qc = useQueryClient();
  const [error, setError] = useState<string | null>(null);
  const [body, setBody]   = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Find peer info from the conversations list (already loaded in parent).
  const conversations = qc.getQueryData<ConversationView[]>(['conversations']);
  const convo = conversations?.find((c) => c.id === conversationId);

  const messages = useInfiniteQuery<CursorPage<MessageView>>({
    queryKey: ['messages', conversationId],
    queryFn: ({ pageParam }) => messagingApi.messages(conversationId, pageParam as string | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => last.nextCursor ?? undefined,
    refetchInterval: REFRESH_INTERVAL_MS,
  });

  // Mark read on view + every read-interval while page is open.
  useEffect(() => {
    let cancelled = false;
    const tick = async () => {
      if (cancelled) return;
      try { await messagingApi.markRead(conversationId); } catch { /* ignore */ }
      qc.invalidateQueries({ queryKey: ['conversations'] });
      qc.invalidateQueries({ queryKey: ['dm-unread'] });
    };
    tick();
    const id = setInterval(tick, READ_INTERVAL_MS);
    return () => { cancelled = true; clearInterval(id); };
  }, [conversationId, qc]);

  // Auto-scroll to newest on first load + when a fresh message comes in.
  const items = (messages.data?.pages ?? []).flatMap((p) => p.items).reverse();
  useEffect(() => {
    const el = scrollRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [items.length]);

  const send = useMutation({
    mutationFn: async () => {
      let attachmentUrl: string | null = null;
      if (pendingFile) {
        attachmentUrl = await messagingApi.uploadAttachment(pendingFile);
      }
      return messagingApi.send(conversationId, body.trim(), attachmentUrl);
    },
    onSuccess: () => {
      setBody('');
      setPendingFile(null);
      setError(null);
      qc.invalidateQueries({ queryKey: ['messages', conversationId] });
      qc.invalidateQueries({ queryKey: ['conversations'] });
    },
    onError: (e) => setError(errorMessage(e)),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!body.trim() && !pendingFile) return;
    send.mutate();
  }

  function onPickFile(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (f) setPendingFile(f);
    e.target.value = '';
  }

  const peer = convo?.peer;
  const peerName = peer?.displayName ?? peer?.username ?? 'Konuşma';

  return (
    <Card padded={false} className="flex flex-col min-h-[70vh] max-h-[78vh] overflow-hidden">
      {/* Thread header */}
      <header className="flex items-center gap-3 border-b border-surface-border px-4 py-3">
        <Link to="/messages" className="md:hidden inline-flex h-8 w-8 items-center justify-center rounded-lg text-ink-700 hover:bg-surface-subtle">
          <ChevronLeft className="h-5 w-5" />
        </Link>
        {peer ? (
          <Link to={`/u/${peer.username}`} className="flex items-center gap-3 group">
            <Avatar name={peerName} src={fullUrl(peer.avatarUrl)} online={isOnline(peer.lastSeenAt)} size="md" />
            <div>
              <p className="font-semibold text-ink-900 group-hover:underline">{peerName}</p>
              <p className="text-xs text-ink-500">
                {isOnline(peer.lastSeenAt)
                  ? <span className="text-emerald-600">çevrimiçi</span>
                  : peer.lastSeenAt ? `son görülme ${relativeTime(peer.lastSeenAt)}` : '@' + peer.username}
              </p>
            </div>
          </Link>
        ) : (
          <p className="font-semibold text-ink-900">{peerName}</p>
        )}
      </header>

      {/* Message list */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-4 space-y-3 bg-surface-base/40">
        {messages.hasNextPage && (
          <div className="flex justify-center">
            <Button variant="ghost" size="sm" onClick={() => messages.fetchNextPage()} disabled={messages.isFetchingNextPage}>
              {messages.isFetchingNextPage ? 'Yükleniyor…' : 'Önceki mesajlar'}
            </Button>
          </div>
        )}
        {messages.isPending && <div className="flex justify-center py-6"><Spinner /></div>}
        {items.map((m) => {
          const mine = m.sender.id === me?.id;
          return (
            <div key={m.id} className={['flex gap-2', mine ? 'justify-end' : 'justify-start'].join(' ')}>
              {!mine && <Avatar name={m.sender.displayName ?? m.sender.username} src={fullUrl(m.sender.avatarUrl)} size="sm" className="self-end" />}
              <div className={['max-w-[78%] flex flex-col', mine ? 'items-end' : 'items-start'].join(' ')}>
                {m.attachmentUrl && (
                  <a href={fullUrl(m.attachmentUrl)!} target="_blank" rel="noreferrer"
                     className="mb-1 max-w-full rounded-xl overflow-hidden border border-surface-border">
                    <img src={fullUrl(m.attachmentUrl)!} alt="ek" className="block max-h-72" />
                  </a>
                )}
                {m.body && (
                  <div className={[
                    'rounded-2xl px-3.5 py-2 text-sm whitespace-pre-wrap break-words shadow-xs',
                    mine
                      ? 'gradient-brand text-white rounded-br-sm'
                      : 'bg-surface-raised border border-surface-border text-ink-900 rounded-bl-sm',
                  ].join(' ')}>
                    {m.body}
                  </div>
                )}
                <div className={['mt-0.5 flex items-center gap-1 text-[11px]', mine ? 'text-ink-400' : 'text-ink-400'].join(' ')}>
                  <span>{relativeTime(m.createdAt)}</span>
                  {mine && (
                    <span title={m.readAt ? 'Görüldü' : 'Gönderildi'}>
                      {m.readAt ? <DoubleCheck className="h-3.5 w-3.5 text-brand-600"/> : <SingleCheck className="h-3.5 w-3.5"/>}
                    </span>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Composer */}
      <form onSubmit={onSubmit} className="border-t border-surface-border bg-surface-raised px-3 py-2.5 space-y-2">
        {pendingFile && (
          <div className="flex items-center gap-2 rounded-lg border border-surface-border bg-surface-subtle px-3 py-1.5 text-xs text-ink-700">
            <span className="truncate">📎 {pendingFile.name}</span>
            <button type="button" onClick={() => setPendingFile(null)} className="ml-auto text-red-600 hover:underline">
              kaldır
            </button>
          </div>
        )}
        {error && <div className="text-xs text-red-600">{error}</div>}
        <div className="flex items-end gap-2">
          <button
            type="button"
            onClick={() => fileRef.current?.click()}
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-ink-500 hover:bg-surface-subtle hover:text-ink-700 transition-colors"
            aria-label="Dosya ekle"
            disabled={send.isPending}
          >
            <PaperclipIcon className="h-5 w-5" />
          </button>
          <input ref={fileRef} type="file" accept="image/*" onChange={onPickFile} className="hidden" />
          <Textarea
            rows={1}
            maxLength={4000}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                if (body.trim() || pendingFile) send.mutate();
              }
            }}
            placeholder="Mesaj yaz… (Enter ile gönder, Shift+Enter satır ekler)"
            className="min-h-[40px] max-h-32"
          />
          <Button type="submit" disabled={send.isPending || (!body.trim() && !pendingFile)}>
            {send.isPending ? 'Gönderiliyor…' : 'Gönder'}
          </Button>
        </div>
      </form>
    </Card>
  );
}

/* ───────────── Icons ───────────── */
function MailIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>);
}
function ChevronLeft({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6"/></svg>);
}
function PaperclipIcon({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21.44 11.05 12.25 20.24a6 6 0 1 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 1 1-2.83-2.83l8.49-8.48"/></svg>);
}
function SingleCheck({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 5 5L20 7"/></svg>);
}
function DoubleCheck({ className }: { className?: string }) {
  return (<svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><path d="M2 12.5 7 17.5 17 7.5"/><path d="M9 12.5 14 17.5 24 7.5" transform="translate(-2,0)"/></svg>);
}
