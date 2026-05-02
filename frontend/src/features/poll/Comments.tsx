import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Button, Card, Spinner, Textarea } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { CommentView, CursorPage, UUID } from '../../lib/types';

export function Comments({ pollId }: { pollId: UUID }) {
  const me = useAuth((s) => s.user);
  const qc = useQueryClient();
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);

  const list = useInfiniteQuery<CursorPage<CommentView>>({
    queryKey: ['comments', pollId],
    queryFn: ({ pageParam }) => commentApi.list(pollId, pageParam as string | undefined),
    initialPageParam: undefined,
    getNextPageParam: (last) => last.nextCursor ?? undefined,
  });

  const create = useMutation({
    mutationFn: (text: string) => commentApi.create(pollId, text),
    onSuccess: () => {
      setBody('');
      setError(null);
      qc.invalidateQueries({ queryKey: ['comments', pollId] });
    },
    onError: (err) => setError(errorMessage(err)),
  });

  const remove = useMutation({
    mutationFn: (id: UUID) => commentApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['comments', pollId] }),
  });

  const items = list.data?.pages.flatMap((p) => p.items) ?? [];

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = body.trim();
    if (trimmed.length === 0) return;
    create.mutate(trimmed);
  }

  return (
    <Card className="space-y-3">
      <h3 className="font-semibold">Yorumlar</h3>

      {me && (
        <form onSubmit={handleSubmit} className="space-y-2">
          <Textarea
            rows={2}
            maxLength={1000}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Yorum yaz…"
          />
          {error && <div className="text-sm text-red-700">{error}</div>}
          <div className="flex justify-end">
            <Button type="submit" disabled={create.isPending || body.trim().length === 0}>
              {create.isPending ? 'Gönderiliyor…' : 'Yorum yap'}
            </Button>
          </div>
        </form>
      )}

      {list.isPending && <div className="flex justify-center"><Spinner /></div>}
      {!list.isPending && items.length === 0 && (
        <p className="text-sm text-slate-500">İlk yorumu sen yap.</p>
      )}

      <ul className="space-y-3">
        {items.map((c) => (
          <li key={c.id} className="border-b border-slate-100 pb-3 last:border-b-0 last:pb-0">
            <div className="flex items-baseline justify-between text-xs text-slate-500">
              <Link to={`/u/${c.author.username}`} className="font-medium text-slate-700 hover:underline">
                @{c.author.username}
              </Link>
              <span>{new Date(c.createdAt).toLocaleString('tr-TR')}</span>
            </div>
            <p className="mt-1 whitespace-pre-wrap text-sm">{c.body}</p>
            {me?.id === c.author.id && (
              <button
                onClick={() => remove.mutate(c.id)}
                disabled={remove.isPending}
                className="mt-1 text-xs text-red-600 hover:underline"
              >
                sil
              </button>
            )}
          </li>
        ))}
      </ul>

      {list.hasNextPage && (
        <div className="flex justify-center">
          <Button variant="outline" onClick={() => list.fetchNextPage()} disabled={list.isFetchingNextPage}>
            {list.isFetchingNextPage ? 'Yükleniyor…' : 'Daha fazla'}
          </Button>
        </div>
      )}
    </Card>
  );
}
