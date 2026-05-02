import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { pollApi } from '../../api/endpoints';
import { Button, Card, Input, Label, Textarea } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { AlicePollDraft, OpenAnswersVisibility, QuestionType, ResultsVisibility } from '../../lib/types';

interface DraftQuestion {
  type: QuestionType;
  prompt: string;
  options: string[];     // SINGLE / MULTIPLE / RANKING
  minSelect: number;     // MULTIPLE
  maxSelect: number;     // MULTIPLE
  scale: number;         // LIKERT
  leftLabel: string;     // LIKERT
  rightLabel: string;    // LIKERT
  maxLength: number;     // OPEN
}

function emptyQuestion(type: QuestionType): DraftQuestion {
  return {
    type,
    prompt: '',
    options: type === 'SINGLE' || type === 'MULTIPLE' || type === 'RANKING' ? ['', ''] : [],
    minSelect: 1,
    maxSelect: 2,
    scale: 5,
    leftLabel: 'Katılmıyorum',
    rightLabel: 'Katılıyorum',
    maxLength: 500,
  };
}

// Map an Alice-generated draft into the form's DraftQuestion shape. Anything
// missing falls back to emptyQuestion defaults so the form stays usable even
// if the model omits a field.
function fromAliceDraft(d: AlicePollDraft): { title: string; description: string; questions: DraftQuestion[] } {
  const valid: QuestionType[] = ['SINGLE', 'MULTIPLE', 'LIKERT', 'RANKING', 'OPEN'];
  const qs: DraftQuestion[] = (d.questions ?? [])
    .filter((q) => valid.includes(q.type))
    .map((q) => {
      const base = emptyQuestion(q.type);
      const p = q.payload ?? {};
      return {
        ...base,
        prompt: q.prompt ?? '',
        options:    p.options    && p.options.length    ? p.options    : base.options,
        minSelect:  p.minSelect  ?? base.minSelect,
        maxSelect:  p.maxSelect  ?? base.maxSelect,
        scale:      p.scale      ?? base.scale,
        leftLabel:  p.leftLabel  ?? base.leftLabel,
        rightLabel: p.rightLabel ?? base.rightLabel,
        maxLength:  p.maxLength  ?? base.maxLength,
      };
    });
  return {
    title: d.title ?? '',
    description: d.description ?? '',
    questions: qs.length ? qs : [emptyQuestion('SINGLE')],
  };
}

function buildPayload(q: DraftQuestion): Record<string, unknown> {
  switch (q.type) {
    case 'SINGLE':
      return { options: q.options.map((o) => o.trim()).filter(Boolean) };
    case 'MULTIPLE':
      return {
        options: q.options.map((o) => o.trim()).filter(Boolean),
        minSelect: q.minSelect,
        maxSelect: q.maxSelect,
      };
    case 'LIKERT':
      return { scale: q.scale, leftLabel: q.leftLabel, rightLabel: q.rightLabel };
    case 'RANKING':
      return { options: q.options.map((o) => o.trim()).filter(Boolean) };
    case 'OPEN':
      return { maxLength: q.maxLength };
  }
}

export function PollCreatePage() {
  const navigate = useNavigate();
  // Alice (or any other entry point) can preload the form by passing a draft
  // through router state: navigate('/polls/new', { state: { seedDraft } }).
  const seed = (useLocation().state as { seedDraft?: AlicePollDraft } | null)?.seedDraft;
  const initial = seed ? fromAliceDraft(seed) : null;

  const [title, setTitle] = useState(initial?.title ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [resultsVisibility, setResultsVisibility] = useState<ResultsVisibility>('AFTER_VOTE');
  const [openAnswersVisibility, setOpenAnswersVisibility] = useState<OpenAnswersVisibility>('PUBLIC');
  const [questions, setQuestions] = useState<DraftQuestion[]>(initial?.questions ?? [emptyQuestion('SINGLE')]);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function updateQ(i: number, patch: Partial<DraftQuestion>) {
    setQuestions((prev) => prev.map((q, idx) => (idx === i ? { ...q, ...patch } : q)));
  }

  function addQuestion() {
    setQuestions((prev) => [...prev, emptyQuestion('SINGLE')]);
  }

  function removeQuestion(i: number) {
    setQuestions((prev) => prev.filter((_, idx) => idx !== i));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const created = await pollApi.create({
        title: title.trim(),
        description: description.trim() || undefined,
        resultsVisibility,
        openAnswersVisibility,
        questions: questions.map((q) => ({
          type: q.type,
          prompt: q.prompt.trim(),
          payload: buildPayload(q),
        })),
      });
      navigate(`/polls/${created.id}`);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="space-y-4" onSubmit={handleSubmit}>
      <Card className="space-y-3">
        <div className="space-y-1">
          <Label htmlFor="title">Başlık</Label>
          <Input
            id="title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            maxLength={280}
            placeholder="Kısa, net bir soru cümlesi"
          />
        </div>
        <div className="space-y-1">
          <Label htmlFor="desc">Açıklama (opsiyonel)</Label>
          <Textarea
            id="desc"
            rows={2}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            maxLength={2000}
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <Label>Sonuç görünürlüğü</Label>
            <select
              value={resultsVisibility}
              onChange={(e) => setResultsVisibility(e.target.value as ResultsVisibility)}
              className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm"
            >
              <option value="AFTER_VOTE">Oylayınca görünür</option>
              <option value="ALWAYS">Herkese açık</option>
              <option value="AUTHOR_ONLY">Sadece sahibi</option>
            </select>
          </div>
          <div className="space-y-1">
            <Label>Açık uçlu cevapların görünürlüğü</Label>
            <select
              value={openAnswersVisibility}
              onChange={(e) => setOpenAnswersVisibility(e.target.value as OpenAnswersVisibility)}
              className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm"
            >
              <option value="PUBLIC">Herkese açık</option>
              <option value="AUTHOR_ONLY">Sadece sahibi</option>
            </select>
          </div>
        </div>
      </Card>

      {questions.map((q, i) => (
        <QuestionCard
          key={i}
          index={i}
          question={q}
          onChange={(patch) => updateQ(i, patch)}
          onRemove={questions.length > 1 ? () => removeQuestion(i) : undefined}
        />
      ))}

      <div className="flex justify-between">
        <Button type="button" variant="outline" onClick={addQuestion}>+ Soru ekle</Button>
        <div className="flex items-center gap-3">
          {error && <span className="text-sm text-red-600">{error}</span>}
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Oluşturuluyor…' : 'Anketi yayımla'}
          </Button>
        </div>
      </div>
    </form>
  );
}

function QuestionCard({
  index, question, onChange, onRemove,
}: {
  index: number;
  question: DraftQuestion;
  onChange: (patch: Partial<DraftQuestion>) => void;
  onRemove?: () => void;
}) {
  return (
    <Card className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-slate-500">Soru {index + 1}</h3>
        {onRemove && (
          <button type="button" onClick={onRemove} className="text-xs text-red-600 hover:underline">
            Kaldır
          </button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label>Tip</Label>
          <select
            value={question.type}
            onChange={(e) => onChange({ ...emptyQuestion(e.target.value as QuestionType), prompt: question.prompt })}
            className="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm"
          >
            <option value="SINGLE">Tek seçim</option>
            <option value="MULTIPLE">Çoklu seçim</option>
            <option value="LIKERT">Likert (ölçek)</option>
            <option value="RANKING">Sıralama</option>
            <option value="OPEN">Açık uçlu</option>
          </select>
        </div>
        <div className="col-span-2 space-y-1">
          <Label>Soru metni</Label>
          <Input
            value={question.prompt}
            onChange={(e) => onChange({ prompt: e.target.value })}
            required
            maxLength={500}
          />
        </div>
      </div>

      <PayloadEditor q={question} onChange={onChange} />
    </Card>
  );
}

function PayloadEditor({ q, onChange }: { q: DraftQuestion; onChange: (p: Partial<DraftQuestion>) => void }) {
  if (q.type === 'SINGLE' || q.type === 'MULTIPLE' || q.type === 'RANKING') {
    return (
      <div className="space-y-2">
        <Label>Seçenekler</Label>
        {q.options.map((opt, i) => (
          <div key={i} className="flex items-center gap-2">
            <Input
              value={opt}
              onChange={(e) => {
                const next = q.options.slice();
                next[i] = e.target.value;
                onChange({ options: next });
              }}
              maxLength={200}
              placeholder={`Seçenek ${i + 1}`}
            />
            {q.options.length > 2 && (
              <button
                type="button"
                onClick={() => onChange({ options: q.options.filter((_, idx) => idx !== i) })}
                className="text-xs text-red-600 hover:underline"
              >
                sil
              </button>
            )}
          </div>
        ))}
        <Button
          type="button"
          variant="outline"
          className="h-8 px-3 text-xs"
          onClick={() => onChange({ options: [...q.options, ''] })}
        >
          + Seçenek ekle
        </Button>
        {q.type === 'MULTIPLE' && (
          <div className="grid grid-cols-2 gap-3 pt-1">
            <div className="space-y-1">
              <Label>En az seçim</Label>
              <Input
                type="number" min={1} max={q.options.length}
                value={q.minSelect}
                onChange={(e) => onChange({ minSelect: Number(e.target.value) })}
              />
            </div>
            <div className="space-y-1">
              <Label>En çok seçim</Label>
              <Input
                type="number" min={q.minSelect} max={q.options.length}
                value={q.maxSelect}
                onChange={(e) => onChange({ maxSelect: Number(e.target.value) })}
              />
            </div>
          </div>
        )}
      </div>
    );
  }

  if (q.type === 'LIKERT') {
    return (
      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1">
          <Label>Ölçek (3-11)</Label>
          <Input type="number" min={3} max={11} value={q.scale}
                 onChange={(e) => onChange({ scale: Number(e.target.value) })} />
        </div>
        <div className="space-y-1">
          <Label>Sol etiket</Label>
          <Input value={q.leftLabel} onChange={(e) => onChange({ leftLabel: e.target.value })} />
        </div>
        <div className="space-y-1">
          <Label>Sağ etiket</Label>
          <Input value={q.rightLabel} onChange={(e) => onChange({ rightLabel: e.target.value })} />
        </div>
      </div>
    );
  }

  // OPEN
  return (
    <div className="space-y-1">
      <Label>Maksimum karakter</Label>
      <Input
        type="number" min={1} max={2000}
        value={q.maxLength}
        onChange={(e) => onChange({ maxLength: Number(e.target.value) })}
      />
    </div>
  );
}
