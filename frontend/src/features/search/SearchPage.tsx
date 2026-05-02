import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { searchApi } from '../../api/endpoints';
import { Card, ErrorBanner, Spinner } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { CursorPage, PollView, UserSummary } from '../../lib/types';

export function SearchPage() {
  const [params, setParams] = useSearchParams();
  const initialQ = params.get('q') ?? '';
  const [term, setTerm] = useState(initialQ);
  const [debounced, setDebounced] = useState(initialQ);

  // 300ms debounce — keeps typing snappy without spamming the backend.
  useEffect(() => {
    const id = setTimeout(() => setDebounced(term.trim()), 300);
    return () => clearTimeout(id);
  }, [term]);

  // Reflect the debounced term in the URL so refresh / back-button keeps state.
  useEffect(() => {
    if (debounced) setParams({ q: debounced }, { replace: true });
  }, [debounced, setParams]);

  const enabled = useMemo(() => debounced.length >= 2, [debounced]);

  const polls = useQuery<CursorPage<PollView>>({
    queryKey: ['search-polls', debounced],
    queryFn: () => searchApi.polls(debounced),
    enabled,
  });
  const users = useQuery<UserSummary[]>({
    queryKey: ['search-users', debounced],
    queryFn: () => searchApi.users(debounced),
    enabled,
  });

  return (
    <div className="space-y-4">
      <input
        type="search"
        autoFocus
        value={term}
        onChange={(e) => setTerm(e.target.value)}
        placeholder="Anket başlığı veya kullanıcı adı…"
        className="h-12 w-full rounded-md border border-slate-300 bg-white px-4 text-base focus:outline-none focus:ring-2 focus:ring-slate-900"
      />

      {!enabled && (
        <Card className="text-center text-sm text-slate-500">En az 2 karakter gir.</Card>
      )}

      {enabled && (polls.isPending || users.isPending) && (
        <div className="flex justify-center py-6"><Spinner /></div>
      )}

      {polls.isError && <ErrorBanner message={errorMessage(polls.error)} />}
      {users.isError && <ErrorBanner message={errorMessage(users.error)} />}

      {users.data && users.data.length > 0 && (
        <section className="space-y-2">
          <h2 className="px-1 text-sm font-semibold text-slate-500">Kullanıcılar</h2>
          {users.data.map((u) => (
            <Link key={u.id} to={`/u/${u.username}`} className="block">
              <Card className="hover:border-slate-400 transition">
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">{u.displayName ?? u.username}</span>
                  <span className="text-xs text-slate-500">@{u.username}</span>
                </div>
              </Card>
            </Link>
          ))}
        </section>
      )}

      {polls.data && polls.data.items.length > 0 && (
        <section className="space-y-2">
          <h2 className="px-1 text-sm font-semibold text-slate-500">Anketler</h2>
          {polls.data.items.map((p) => (
            <Link key={p.id} to={`/polls/${p.id}`} className="block">
              <Card className="hover:border-slate-400 transition">
                <div className="flex items-baseline justify-between">
                  <span className="font-medium">{p.title}</span>
                  <span className="text-xs text-slate-400">@{p.author.username}</span>
                </div>
                {p.description && <p className="mt-1 text-sm text-slate-600 line-clamp-2">{p.description}</p>}
              </Card>
            </Link>
          ))}
        </section>
      )}

      {enabled && !polls.isPending && !users.isPending
       && (polls.data?.items.length ?? 0) === 0
       && (users.data?.length ?? 0) === 0 && (
        <Card className="text-center text-sm text-slate-500">Sonuç yok.</Card>
      )}
    </div>
  );
}
