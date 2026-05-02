import { useEffect } from 'react';
import type { QuestionView } from '../../../lib/types';
import { Button } from '../../../components/ui';

type Payload = { ranking: number[] };

/**
 * No drag-and-drop dependency for MVP — users move items up/down with the
 * keyboard-style arrows. Functionally complete; richer DnD can come later.
 */
export function VoteRanking({
  question,
  value,
  onChange,
}: {
  question: QuestionView;
  value: Payload | undefined;
  onChange: (v: Payload) => void;
}) {
  const options = (question.payload.options as string[]) ?? [];
  const order = value?.ranking ?? options.map((_, i) => i);

  // If we haven't initialized yet, lift the default into parent state once.
  useEffect(() => {
    if (!value) onChange({ ranking: options.map((_, i) => i) });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function move(idx: number, delta: number) {
    const target = idx + delta;
    if (target < 0 || target >= order.length) return;
    const next = order.slice();
    [next[idx], next[target]] = [next[target], next[idx]];
    onChange({ ranking: next });
  }

  return (
    <ol className="space-y-2">
      {order.map((optionIndex, position) => (
        <li
          key={optionIndex}
          className="flex items-center gap-3 rounded-md border border-slate-200 bg-white px-3 py-2"
        >
          <span className="w-6 text-sm font-semibold text-slate-500">{position + 1}.</span>
          <span className="flex-1 text-sm">{options[optionIndex]}</span>
          <Button type="button" variant="outline" className="h-8 px-2" onClick={() => move(position, -1)} disabled={position === 0}>↑</Button>
          <Button type="button" variant="outline" className="h-8 px-2" onClick={() => move(position, 1)} disabled={position === order.length - 1}>↓</Button>
        </li>
      ))}
    </ol>
  );
}

export function ResultRanking({ data }: { data: Record<string, unknown> }) {
  const options = (data.options as Array<{
    label: string;
    bordaScore: number;
    averagePosition: number | null;
  }>) ?? [];
  const voters = (data.totalVoters as number) ?? 0;

  return (
    <div className="space-y-2">
      <div className="text-xs text-slate-500">{voters} oy veren · Borda sırasına göre</div>
      <ol className="space-y-1">
        {options.map((row, i) => (
          <li key={i} className="flex items-center justify-between rounded-md bg-slate-50 px-3 py-2 text-sm">
            <span><span className="font-semibold">{i + 1}.</span> {row.label}</span>
            <span className="text-xs text-slate-500">
              Skor: {row.bordaScore}
              {row.averagePosition != null && <> · Ort. sıra: {row.averagePosition.toFixed(2)}</>}
            </span>
          </li>
        ))}
      </ol>
    </div>
  );
}
