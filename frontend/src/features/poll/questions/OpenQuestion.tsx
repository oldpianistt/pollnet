import type { QuestionView } from '../../../lib/types';
import { Textarea } from '../../../components/ui';

type Payload = { text: string };

export function VoteOpen({
  question,
  value,
  onChange,
}: {
  question: QuestionView;
  value: Payload | undefined;
  onChange: (v: Payload) => void;
}) {
  const maxLength = (question.payload.maxLength as number | undefined) ?? 500;
  const current = value?.text ?? '';
  return (
    <div className="space-y-1">
      <Textarea
        rows={4}
        maxLength={maxLength}
        value={current}
        onChange={(e) => onChange({ text: e.target.value })}
        placeholder="Cevabını yaz…"
      />
      <div className="text-right text-xs text-slate-500">{current.length}/{maxLength}</div>
    </div>
  );
}

export function ResultOpen({ data }: { data: Record<string, unknown> }) {
  const count = (data.count as number) ?? 0;
  const items = (data.items as Array<{ answerId: string; text: string; createdAt: string }> | undefined) ?? null;
  return (
    <div className="space-y-2">
      <div className="text-xs text-slate-500">{count} cevap</div>
      {items === null ? (
        <p className="text-sm text-slate-500">Açık uçlu cevaplar yalnızca anket sahibine görünür.</p>
      ) : items.length === 0 ? (
        <p className="text-sm text-slate-500">Henüz cevap yok.</p>
      ) : (
        <ul className="space-y-1 max-h-64 overflow-auto">
          {items.map((it) => (
            <li key={it.answerId} className="rounded-md bg-slate-50 px-3 py-2 text-sm">
              {it.text}
              <div className="mt-0.5 text-xs text-slate-400">
                {new Date(it.createdAt).toLocaleString('tr-TR')}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
