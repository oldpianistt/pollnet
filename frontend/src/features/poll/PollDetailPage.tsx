import { useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { pollApi } from '../../api/endpoints';
import { useAuth } from '../../store/auth';
import { Button, Card, ErrorBanner, Spinner } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { PollView, QuestionType, QuestionView, ResultsView, UUID } from '../../lib/types';
import { Comments } from './Comments';
import { VoteSingle, ResultSingle } from './questions/SingleQuestion';
import { VoteMultiple, ResultMultiple } from './questions/MultipleQuestion';
import { VoteLikert, ResultLikert } from './questions/LikertQuestion';
import { VoteRanking, ResultRanking } from './questions/RankingQuestion';
import { VoteOpen, ResultOpen } from './questions/OpenQuestion';
import { AxiosError } from 'axios';

type AnswerMap = Record<UUID, Record<string, unknown>>;

export function PollDetailPage() {
  const { id } = useParams<{ id: UUID }>();
  const me = useAuth((s) => s.user);
  const navigate = useNavigate();
  const qc = useQueryClient();

  const pollQuery = useQuery<PollView>({
    queryKey: ['poll', id],
    queryFn: () => pollApi.get(id!),
    enabled: !!id,
  });

  // Always *attempt* results — backend tells us 403 if visibility denies it,
  // and we just hide the section in that case.
  const resultsQuery = useQuery<ResultsView, Error>({
    queryKey: ['results', id],
    queryFn: () => pollApi.results(id!),
    enabled: !!id,
    retry: false,
  });

  if (pollQuery.isPending) return <div className="flex justify-center py-10"><Spinner /></div>;
  if (pollQuery.isError) return <ErrorBanner message={errorMessage(pollQuery.error)} />;

  const poll = pollQuery.data!;
  const isAuthor = me?.id === poll.author.id;
  const resultsHidden = resultsHiddenReason(resultsQuery.error);
  const results = resultsQuery.data;
  const showResults = poll.viewerHasAnswered || isAuthor || (results && !resultsQuery.isError);

  return (
    <div className="space-y-4">
      <Card>
        <div className="flex items-baseline justify-between">
          <Link to={`/u/${poll.author.username}`} className="text-sm text-slate-500 hover:underline">
            @{poll.author.username}
          </Link>
          <span className="text-xs text-slate-400">{new Date(poll.createdAt).toLocaleString('tr-TR')}</span>
        </div>
        <h1 className="mt-1 text-2xl font-bold">{poll.title}</h1>
        {poll.description && <p className="mt-1 text-sm text-slate-700">{poll.description}</p>}
        <div className="mt-2 flex gap-2 text-xs text-slate-500">
          <Tag>Sonuç: {labelResultsVisibility(poll.resultsVisibility)}</Tag>
          {poll.questions.some((q) => q.type === 'OPEN') && (
            <Tag>Açık cevap: {poll.openAnswersVisibility === 'PUBLIC' ? 'Herkese açık' : 'Sadece sahip'}</Tag>
          )}
          {isAuthor && (
            <Button
              variant="danger"
              className="ml-auto h-7 px-2 text-xs"
              onClick={async () => {
                if (!confirm('Anketi silmek istediğine emin misin?')) return;
                await pollApi.delete(poll.id);
                qc.invalidateQueries({ queryKey: ['feed'] });
                navigate('/');
              }}
            >
              Sil
            </Button>
          )}
        </div>
      </Card>

      {!poll.viewerHasAnswered && !isAuthor && me && <VotingForm poll={poll} />}

      {showResults && results && (
        <Card className="space-y-6">
          <h2 className="text-lg font-semibold">Sonuçlar</h2>
          {results.results.map((r, i) => (
            <div key={r.questionId} className="space-y-2">
              <div className="text-sm font-medium">
                <span className="text-slate-400">{i + 1}.</span> {r.prompt}
              </div>
              <ResultBlock type={r.type} data={r.data} />
            </div>
          ))}
        </Card>
      )}

      {!showResults && resultsHidden && (
        <Card className="text-sm text-slate-600">{resultsHidden}</Card>
      )}

      <Comments pollId={poll.id} />
    </div>
  );
}

function Tag({ children }: { children: React.ReactNode }) {
  return <span className="rounded bg-slate-100 px-2 py-0.5">{children}</span>;
}

function labelResultsVisibility(v: PollView['resultsVisibility']): string {
  if (v === 'ALWAYS') return 'Herkese açık';
  if (v === 'AFTER_VOTE') return 'Oylayınca görünür';
  return 'Sadece sahip';
}

function resultsHiddenReason(err: unknown): string | null {
  if (err instanceof AxiosError) {
    const code = (err.response?.data as { code?: string } | undefined)?.code;
    if (code === 'VOTE_REQUIRED') return 'Sonuçları görmek için önce oyla.';
    if (code === 'RESULTS_HIDDEN') return 'Sonuçlar yalnızca anket sahibine görünür.';
  }
  return null;
}

function VotingForm({ poll }: { poll: PollView }) {
  const qc = useQueryClient();
  const [answers, setAnswers] = useState<AnswerMap>({});
  const [error, setError] = useState<string | null>(null);

  const mutate = useMutation({
    mutationFn: () =>
      pollApi.submitAnswers(
        poll.id,
        poll.questions.map((q) => ({ questionId: q.id, payload: answers[q.id]! })),
      ),
    onSuccess: () => {
      setError(null);
      qc.invalidateQueries({ queryKey: ['poll', poll.id] });
      qc.invalidateQueries({ queryKey: ['results', poll.id] });
      qc.invalidateQueries({ queryKey: ['feed'] });
    },
    onError: (err) => setError(errorMessage(err)),
  });

  const allAnswered = poll.questions.every((q) => isAnswered(q, answers[q.id]));

  function setOne(qId: UUID, payload: Record<string, unknown>) {
    setAnswers((prev) => ({ ...prev, [qId]: payload }));
  }

  return (
    <Card className="space-y-6">
      {poll.questions.map((q, i) => (
        <div key={q.id} className="space-y-2">
          <div className="text-sm font-medium">
            <span className="text-slate-400">{i + 1}.</span> {q.prompt}
          </div>
          <VoteBlock
            type={q.type}
            question={q}
            value={answers[q.id]}
            onChange={(v) => setOne(q.id, v)}
          />
        </div>
      ))}
      {error && <ErrorBanner message={error} />}
      <div className="flex justify-end">
        <Button onClick={() => mutate.mutate()} disabled={!allAnswered || mutate.isPending}>
          {mutate.isPending ? 'Gönderiliyor…' : 'Oyla'}
        </Button>
      </div>
    </Card>
  );
}

function isAnswered(q: QuestionView, payload: Record<string, unknown> | undefined): boolean {
  if (!payload) return q.type === 'RANKING'; // ranking has a default initial order
  switch (q.type) {
    case 'SINGLE':   return typeof payload.selectedIndex === 'number';
    case 'MULTIPLE': {
      const arr = payload.selectedIndices as number[] | undefined;
      const min = (q.payload.minSelect as number | undefined) ?? 1;
      return Array.isArray(arr) && arr.length >= min;
    }
    case 'LIKERT':   return typeof payload.value === 'number';
    case 'RANKING':  return Array.isArray(payload.ranking);
    case 'OPEN':     return typeof payload.text === 'string' && payload.text.trim().length > 0;
  }
}

function VoteBlock({
  type, question, value, onChange,
}: {
  type: QuestionType;
  question: QuestionView;
  value: Record<string, unknown> | undefined;
  onChange: (v: Record<string, unknown>) => void;
}) {
  switch (type) {
    case 'SINGLE':   return <VoteSingle   question={question} value={value as never} onChange={onChange as never} />;
    case 'MULTIPLE': return <VoteMultiple question={question} value={value as never} onChange={onChange as never} />;
    case 'LIKERT':   return <VoteLikert   question={question} value={value as never} onChange={onChange as never} />;
    case 'RANKING':  return <VoteRanking  question={question} value={value as never} onChange={onChange as never} />;
    case 'OPEN':     return <VoteOpen     question={question} value={value as never} onChange={onChange as never} />;
  }
}

function ResultBlock({ type, data }: { type: QuestionType; data: Record<string, unknown> }) {
  switch (type) {
    case 'SINGLE':   return <ResultSingle data={data} />;
    case 'MULTIPLE': return <ResultMultiple data={data} />;
    case 'LIKERT':   return <ResultLikert data={data} />;
    case 'RANKING':  return <ResultRanking data={data} />;
    case 'OPEN':     return <ResultOpen data={data} />;
  }
}
