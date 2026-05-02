import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { aliceApi } from '../../api/endpoints';
import { Badge, Button, Card, ErrorBanner, EmptyState, Label, Spinner, Textarea } from '../../components/ui';
import { errorMessage } from '../../lib/errors';
import type { AlicePollDraft, QuestionType } from '../../lib/types';

const QUESTION_TYPE_LABEL: Record<QuestionType, string> = {
  SINGLE:   'Tek seçim',
  MULTIPLE: 'Çoklu seçim',
  LIKERT:   'Likert',
  RANKING:  'Sıralama',
  OPEN:     'Açık uçlu',
};

const EXAMPLES = [
  'Takımın yeni bir programlama dili öğrenmeye ne kadar açık?',
  'Şirket içi öğle yemeği menüsü hakkında bir anket',
  'Kullanıcıların yeni mesajlaşma özelliklerini değerlendirsin',
  'Hafta sonu etkinlik tercihi anketi (kamp, sinema, tiyatro)',
];

export function AlicePage() {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [draft, setDraft] = useState<AlicePollDraft | null>(null);

  const statusQ = useQuery({
    queryKey: ['alice', 'status'],
    queryFn: aliceApi.status,
    staleTime: 60_000,
  });

  const suggest = useMutation({
    mutationFn: (p: string) => aliceApi.suggestPoll(p),
    onSuccess: (d) => setDraft(d),
  });

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!prompt.trim()) return;
    suggest.mutate(prompt.trim());
  }

  function useDraft() {
    if (!draft) return;
    navigate('/polls/new', { state: { seedDraft: draft } });
  }

  const disabled = statusQ.data?.enabled === false;

  return (
    <div className="space-y-5">
      <header className="space-y-1">
        <div className="flex items-center gap-2">
          <span className="text-3xl" aria-hidden>✨</span>
          <h1 className="text-2xl font-bold tracking-tightish text-ink-900">Alice</h1>
          <Badge tone="brand">AI</Badge>
        </div>
        <p className="text-sm text-ink-600">
          Aklındaki konuyu yaz, Alice senin için bir anket taslağı hazırlasın. Beğendiğinde tek tıkla
          oluşturma sayfasına aktar — istersen düzenleyebilirsin.
        </p>
      </header>

      {disabled && (
        <Card className="border-amber-300 bg-amber-50 text-amber-900">
          <p className="text-sm">
            <strong>Alice şu an kapalı.</strong> Sunucuda <code>ANTHROPIC_API_KEY</code> tanımlı değil.
            Ayarladıktan sonra modülü tekrar başlatabilirsin.
          </p>
        </Card>
      )}

      <Card>
        <form className="space-y-3" onSubmit={onSubmit}>
          <div className="space-y-1.5">
            <Label htmlFor="alice-prompt">Konu / istem</Label>
            <Textarea
              id="alice-prompt"
              rows={4}
              maxLength={2000}
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Ör: 'Hafta sonu etkinlik tercihi için 3-4 soruluk hızlı bir anket'"
              disabled={disabled || suggest.isPending}
            />
            <div className="flex items-center justify-between text-xs text-ink-400">
              <span>{prompt.length}/2000</span>
              <span>Türkçe yazarsan Türkçe, İngilizce yazarsan İngilizce taslak alırsın.</span>
            </div>
          </div>

          <ErrorBanner message={suggest.isError ? errorMessage(suggest.error) : null} />

          <div className="flex flex-wrap gap-2">
            {EXAMPLES.map((ex) => (
              <button
                key={ex}
                type="button"
                onClick={() => setPrompt(ex)}
                className="rounded-full border border-surface-border bg-surface-raised px-3 py-1 text-xs text-ink-700 hover:border-brand-300 hover:text-brand-700"
                disabled={disabled || suggest.isPending}
              >
                {ex}
              </button>
            ))}
          </div>

          <div className="flex justify-end pt-1">
            <Button type="submit" disabled={disabled || suggest.isPending || !prompt.trim()}>
              {suggest.isPending ? 'Alice düşünüyor…' : '✨ Anket öner'}
            </Button>
          </div>
        </form>
      </Card>

      {suggest.isPending && (
        <Card>
          <div className="flex items-center gap-3 py-4">
            <Spinner />
            <div className="text-sm text-ink-600">
              Alice konuya göre dengeli bir anket taslağı hazırlıyor. Birkaç saniye sürebilir.
            </div>
          </div>
        </Card>
      )}

      {!suggest.isPending && draft && <DraftPreview draft={draft} onUse={useDraft} />}

      {!suggest.isPending && !draft && !suggest.isError && (
        <EmptyState
          icon="🪄"
          title="Henüz taslak yok"
          description="Yukarıya bir konu yaz, Alice anketi oluştursun."
        />
      )}
    </div>
  );
}

function DraftPreview({ draft, onUse }: { draft: AlicePollDraft; onUse: () => void }) {
  return (
    <Card className="space-y-4">
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <h2 className="text-lg font-semibold text-ink-900">{draft.title || 'Başlıksız anket'}</h2>
          {draft.description && (
            <p className="text-sm text-ink-600">{draft.description}</p>
          )}
        </div>
        <Button onClick={onUse}>Bu anketi kullan →</Button>
      </div>

      <ol className="space-y-3">
        {draft.questions.map((q, i) => (
          <li key={i} className="rounded-lg border border-surface-border bg-surface-subtle/40 p-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-ink-400">Soru {i + 1}</span>
              <Badge tone="neutral">{QUESTION_TYPE_LABEL[q.type] ?? q.type}</Badge>
            </div>
            <p className="mt-1.5 font-medium text-ink-900">{q.prompt}</p>
            <PayloadPreview type={q.type} payload={q.payload} />
          </li>
        ))}
      </ol>

      <p className="text-xs text-ink-500">
        Aktardığında bütün alanları düzenleyebilirsin — sayılar, etiketler, seçenekler dahil.
      </p>
    </Card>
  );
}

function PayloadPreview({ type, payload }: { type: QuestionType; payload: AlicePollDraft['questions'][number]['payload'] }) {
  if (type === 'OPEN') {
    return <p className="mt-2 text-xs text-ink-500">Açık uçlu cevap — en fazla {payload.maxLength ?? 500} karakter.</p>;
  }
  if (type === 'LIKERT') {
    return (
      <p className="mt-2 text-xs text-ink-500">
        {payload.scale ?? 5} basamaklı ölçek — “{payload.leftLabel ?? 'Sol'}” ↔ “{payload.rightLabel ?? 'Sağ'}”
      </p>
    );
  }
  const opts = payload.options ?? [];
  if (!opts.length) return null;
  return (
    <ul className="mt-2 space-y-1">
      {opts.map((o, i) => (
        <li key={i} className="text-sm text-ink-700">
          <span className="mr-1.5 text-ink-400">•</span>{o}
        </li>
      ))}
      {type === 'MULTIPLE' && (
        <li className="pt-1 text-xs text-ink-500">
          {payload.minSelect ?? 1}–{payload.maxSelect ?? opts.length} seçim
        </li>
      )}
    </ul>
  );
}
