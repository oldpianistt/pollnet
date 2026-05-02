import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip } from 'recharts';
import type { QuestionView } from '../../../lib/types';
import { cn } from '../../../lib/cn';

type Payload = { value: number };

export function VoteLikert({
  question,
  value,
  onChange,
}: {
  question: QuestionView;
  value: Payload | undefined;
  onChange: (v: Payload) => void;
}) {
  const scale = (question.payload.scale as number) ?? 5;
  const left = (question.payload.leftLabel as string) ?? '';
  const right = (question.payload.rightLabel as string) ?? '';

  return (
    <div>
      <div className="flex items-center justify-between text-xs text-slate-500">
        <span>{left}</span>
        <span>{right}</span>
      </div>
      <div className="mt-2 grid gap-2" style={{ gridTemplateColumns: `repeat(${scale}, minmax(0, 1fr))` }}>
        {Array.from({ length: scale }, (_, i) => i + 1).map((n) => (
          <button
            key={n}
            type="button"
            onClick={() => onChange({ value: n })}
            className={cn(
              'h-12 rounded-md border text-sm font-medium transition',
              value?.value === n
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-300 bg-white text-slate-700 hover:border-slate-500',
            )}
          >
            {n}
          </button>
        ))}
      </div>
    </div>
  );
}

export function ResultLikert({ data }: { data: Record<string, unknown> }) {
  const scale = (data.scale as number) ?? 5;
  const distribution = (data.distribution as Record<string, number>) ?? {};
  const mean = (data.mean as number) ?? 0;
  const stddev = (data.stddev as number) ?? 0;
  const count = (data.count as number) ?? 0;

  const chart = Array.from({ length: scale }, (_, i) => ({
    value: String(i + 1),
    count: distribution[String(i + 1)] ?? 0,
  }));

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-4 text-xs text-slate-600">
        <span>{count} cevap</span>
        <span>Ortalama: <strong>{mean.toFixed(2)}</strong></span>
        <span>Std sapma: <strong>{stddev.toFixed(2)}</strong></span>
      </div>
      <div className="h-[200px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={chart}>
            <XAxis dataKey="value" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Bar dataKey="count" fill="#0f172a" radius={4} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
