import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../api/endpoints';
import { cn } from '../../lib/cn';
import type { CursorPage, NotificationView } from '../../lib/types';

const POLL_INTERVAL_MS = 30_000;

export function NotificationsBell() {
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const unread = useQuery<number>({
    queryKey: ['notif-unread'],
    queryFn: notificationApi.unreadCount,
    refetchInterval: POLL_INTERVAL_MS,
    refetchIntervalInBackground: false,
  });

  const list = useQuery<CursorPage<NotificationView>>({
    queryKey: ['notifications'],
    queryFn: () => notificationApi.list(),
    enabled: open,
  });

  const markAllRead = useMutation({
    mutationFn: notificationApi.markAllRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notif-unread'] });
      qc.invalidateQueries({ queryKey: ['notifications'] });
    },
  });

  // Click outside closes the dropdown.
  useEffect(() => {
    if (!open) return;
    function onDoc(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onDoc);
    return () => document.removeEventListener('mousedown', onDoc);
  }, [open]);

  const count = unread.data ?? 0;
  const items = list.data?.items ?? [];

  return (
    <div ref={wrapperRef} className="relative">
      <button
        type="button"
        aria-label="Bildirimler"
        onClick={() => setOpen((v) => !v)}
        className="relative inline-flex h-10 w-10 items-center justify-center rounded-md text-slate-700 hover:bg-slate-100"
      >
        <BellIcon />
        {count > 0 && (
          <span className="absolute -right-0.5 -top-0.5 inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full z-30 mt-1 w-80 max-w-[calc(100vw-2rem)] rounded-md border border-slate-200 bg-white shadow-lg">
          <div className="flex items-center justify-between border-b border-slate-100 px-3 py-2">
            <span className="text-sm font-semibold">Bildirimler</span>
            {count > 0 && (
              <button
                onClick={() => markAllRead.mutate()}
                disabled={markAllRead.isPending}
                className="text-xs font-medium text-slate-600 hover:underline disabled:opacity-50"
              >
                Hepsini okundu yap
              </button>
            )}
          </div>
          <div className="max-h-96 overflow-y-auto">
            {list.isPending && open && <div className="px-3 py-6 text-center text-sm text-slate-500">Yükleniyor…</div>}
            {!list.isPending && items.length === 0 && (
              <div className="px-3 py-6 text-center text-sm text-slate-500">Bildirim yok.</div>
            )}
            <ul>
              {items.map((n) => (
                <li key={n.id} className={cn(
                  'border-b border-slate-50 px-3 py-2 text-sm last:border-b-0',
                  !n.readAt && 'bg-blue-50/40',
                )}>
                  <NotificationRow n={n} onClose={() => setOpen(false)} />
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}

function NotificationRow({ n, onClose }: { n: NotificationView; onClose: () => void }) {
  const actor = (n.payload.actorDisplayName as string | undefined) ?? (n.payload.actorUsername as string | undefined) ?? 'Birisi';
  const actorUsername = n.payload.actorUsername as string | undefined;
  const pollId = n.payload.pollId as string | undefined;
  const pollTitle = (n.payload.pollTitle as string | undefined) ?? '';
  const created = new Date(n.createdAt).toLocaleString('tr-TR');

  let content: React.ReactNode;
  let to: string | null = null;
  switch (n.type) {
    case 'NEW_FOLLOWER':
      content = <><strong>{actor}</strong> seni takip etmeye başladı</>;
      if (actorUsername) to = `/u/${actorUsername}`;
      break;
    case 'POLL_ANSWERED':
      content = <><strong>{actor}</strong> "{pollTitle}" anketini yanıtladı</>;
      if (pollId) to = `/polls/${pollId}`;
      break;
    case 'POLL_COMMENTED':
      content = <><strong>{actor}</strong> "{pollTitle}" anketine yorum yaptı</>;
      if (pollId) to = `/polls/${pollId}`;
      break;
  }

  const body = (
    <>
      <div className="text-slate-800">{content}</div>
      <div className="mt-0.5 text-xs text-slate-400">{created}</div>
    </>
  );

  return to ? (
    <Link to={to} onClick={onClose} className="block hover:underline">{body}</Link>
  ) : (
    <div>{body}</div>
  );
}

function BellIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
      <path d="M10 21a2 2 0 0 0 4 0"/>
    </svg>
  );
}
