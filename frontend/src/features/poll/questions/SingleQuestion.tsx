import { BarChart, Bar, XAxis, YAxis, ResponsiveContainer, Tooltip, LabelList } from 'recharts';
import type { QuestionView } from '../../../lib/types';
import { cn } from '../../../lib/cn';

type Payload = { selectedIndex: number };

export function VoteSingle({
  question,
  value,
  onChange,
}: {
  question: QuestionView;
  value: Payload | undefined;
  onChange: (v: Payload) => void;
}) {
  const options = (question.payload.options as string[]) ?? [];
  return (
    <div className="space-y-2">
      {options.map((opt, i) => (
        <label
          key={i}
          className={cn(
            'flex items-center gap-3 rounded-md border px-3 py-2 cursor-pointer transition',
            value?.selectedIndex === i
              ? 'border-slate-900 bg-slate-50'
              : 'border-slate-200 hover:border-slate-400',
          )}
        >
          <input
            type="radio"
            name={`q-${question.id}`}
            checked={value?.selectedIndex === i}
            onChange={() => onChange({ selectedIndex: i })}
            className="accent-slate-900"
          />
          <span className="text-sm">{opt}</span>
        </label>
      ))}
    </div>
  );
}

export function ResultSingle({ data }: { data: Record<string, unknown> }) {
  const options = (data.options as Array<{ label: string; count: number; percent: number }>) ?? [];
  const total = (data.totalVotes as number) ?? 0;
  return (
    <div className="space-y-2">
      <div className="text-xs text-slate-500">{total} oy</div>
      <div className="h-[220px]">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={options} layout="vertical" margin={{ left: 0, right: 48 }}>
            <XAxis type="number" hide />
            <YAxis dataKey="label" type="category" width={120} tick={{ fontSize: 12 }} />
            <Tooltip formatter={(v: number) => [`${v} oy`, 'Sayı']} />
            <Bar dataKey="count" fill="#0f172a" radius={4}>
              <LabelList dataKey="percent" position="right" formatter={(v: number) => `${v.toFixed(0)}%`} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
