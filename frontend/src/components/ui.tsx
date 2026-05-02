import { forwardRef, type ButtonHTMLAttributes, type InputHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { cn } from '../lib/cn';

/* ───────────── Button ───────────── */

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'outline' | 'danger';
type ButtonSize    = 'sm' | 'md' | 'lg' | 'icon';

const buttonVariants: Record<ButtonVariant, string> = {
  primary:   'gradient-brand text-white hover:brightness-105 active:brightness-95 shadow-sm hover:shadow-md',
  secondary: 'bg-ink-900 text-white hover:bg-ink-700 shadow-sm',
  outline:   'border border-surface-border bg-surface-raised text-ink-900 hover:bg-surface-subtle hover:border-surface-border-strong',
  ghost:     'bg-transparent text-ink-700 hover:bg-surface-subtle hover:text-ink-900',
  danger:    'bg-red-600 text-white hover:bg-red-700 shadow-sm',
};

const buttonSizes: Record<ButtonSize, string> = {
  sm:   'h-8  px-3 text-sm',
  md:   'h-10 px-4 text-sm',
  lg:   'h-12 px-6 text-base',
  icon: 'h-10 w-10',
};

export const Button = forwardRef<HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: ButtonVariant; size?: ButtonSize }>(
  ({ className, variant = 'primary', size = 'md', ...props }, ref) => (
    <button
      ref={ref}
      className={cn(
        'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded font-medium',
        'transition-all duration-150 ease-out-soft',
        'disabled:opacity-50 disabled:pointer-events-none',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 focus-visible:ring-offset-surface-base',
        buttonVariants[variant],
        buttonSizes[size],
        className,
      )}
      {...props}
    />
  ),
);
Button.displayName = 'Button';

/* ───────────── Input ───────────── */

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        'flex h-11 w-full rounded-lg border border-surface-border bg-surface-raised px-3.5 text-sm text-ink-900',
        'placeholder:text-ink-400 transition-all duration-150',
        'hover:border-surface-border-strong',
        'focus:outline-none focus:border-brand-500 focus:shadow-ring',
        'disabled:bg-surface-subtle disabled:text-ink-400',
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = 'Input';

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        'flex w-full rounded-lg border border-surface-border bg-surface-raised px-3.5 py-2.5 text-sm text-ink-900',
        'placeholder:text-ink-400 transition-all duration-150 resize-y',
        'hover:border-surface-border-strong',
        'focus:outline-none focus:border-brand-500 focus:shadow-ring',
        className,
      )}
      {...props}
    />
  ),
);
Textarea.displayName = 'Textarea';

export function Label({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return (
    <label
      className={cn('text-sm font-medium text-ink-700', className)}
      {...props}
    />
  );
}

/* ───────────── Card ───────────── */

export function Card({
  className, interactive = false, padded = true, ...props
}: React.HTMLAttributes<HTMLDivElement> & { interactive?: boolean; padded?: boolean }) {
  return (
    <div
      className={cn(
        'rounded-xl border border-surface-border bg-surface-raised shadow-xs',
        padded && 'p-5',
        interactive && 'transition-all duration-150 ease-out-soft hover:border-surface-border-strong hover:shadow-md hover:-translate-y-px cursor-pointer',
        className,
      )}
      {...props}
    />
  );
}

/* ───────────── Spinner ───────────── */

export function Spinner({ className }: { className?: string }) {
  return (
    <div
      className={cn('h-4 w-4 animate-spin rounded-full border-2 border-surface-border border-t-brand-600', className)}
      aria-label="loading"
    />
  );
}

/* ───────────── ErrorBanner ───────────── */

export function ErrorBanner({ message }: { message: string | null | undefined }) {
  if (!message) return null;
  return (
    <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 px-3.5 py-2.5 text-sm text-red-700 animate-fade-in">
      <svg className="h-4 w-4 mt-0.5 flex-shrink-0" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16zm-1-9a1 1 0 1 1 2 0v3a1 1 0 1 1-2 0V9zm1 6a1 1 0 1 1 0-2 1 1 0 0 1 0 2z" clipRule="evenodd" />
      </svg>
      <span>{message}</span>
    </div>
  );
}

/* ───────────── Badge ───────────── */

type BadgeTone = 'neutral' | 'brand' | 'success' | 'warning' | 'info';

export function Badge({
  tone = 'neutral', className, children, ...props
}: React.HTMLAttributes<HTMLSpanElement> & { tone?: BadgeTone }) {
  const toneClass: Record<BadgeTone, string> = {
    neutral: 'bg-surface-subtle text-ink-700 border-surface-border',
    brand:   'bg-brand-50 text-brand-700 border-brand-100',
    success: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    warning: 'bg-amber-50 text-amber-700 border-amber-100',
    info:    'bg-sky-50 text-sky-700 border-sky-100',
  };
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-xs font-medium',
        toneClass[tone],
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}

/* ───────────── Avatar (initials with deterministic gradient) ───────────── */

const GRADIENTS = [
  'from-violet-500 to-fuchsia-500',
  'from-blue-500 to-cyan-500',
  'from-emerald-500 to-teal-500',
  'from-amber-500 to-orange-500',
  'from-rose-500 to-pink-500',
  'from-indigo-500 to-purple-500',
];

function pickGradient(seed: string): string {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) | 0;
  return GRADIENTS[Math.abs(h) % GRADIENTS.length];
}

export function Avatar({
  name, src, size = 'md', online, className,
}: {
  name: string;
  src?: string | null;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl';
  online?: boolean;
  className?: string;
}) {
  const initials = name
    .split(/\s+/)
    .map((p) => p[0])
    .filter(Boolean)
    .slice(0, 2)
    .join('')
    .toUpperCase() || '?';

  const sizeClass = {
    xs: 'h-6  w-6  text-[10px]',
    sm: 'h-8  w-8  text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-14 w-14 text-base',
    xl: 'h-24 w-24 text-2xl',
  }[size];

  const dotSize = {
    xs: 'h-1.5 w-1.5',
    sm: 'h-2   w-2',
    md: 'h-2.5 w-2.5',
    lg: 'h-3   w-3',
    xl: 'h-4   w-4',
  }[size];

  return (
    // `rounded-full` on the wrapper so rings passed via className (e.g. the
    // ring-4 used on ProfilePage) follow the circle, not the inline-flex box.
    <span className={cn('relative inline-flex flex-shrink-0 rounded-full', className)}>
      {src ? (
        <img
          src={src}
          alt={name}
          className={cn(
            'rounded-full object-cover ring-2 ring-surface-raised shadow-sm',
            sizeClass,
          )}
          loading="lazy"
        />
      ) : (
        <span
          className={cn(
            'inline-flex items-center justify-center rounded-full font-semibold text-white',
            'bg-gradient-to-br shadow-sm ring-2 ring-surface-raised',
            pickGradient(name),
            sizeClass,
          )}
          aria-label={name}
        >
          {initials}
        </span>
      )}
      {online && (
        <span
          className={cn(
            'absolute right-0 bottom-0 rounded-full bg-emerald-500 ring-2 ring-surface-raised',
            dotSize,
          )}
          aria-label="çevrimiçi"
        />
      )}
    </span>
  );
}

/** "online if last_seen within last 2 minutes". Single source of truth so list/profile agree. */
export function isOnline(lastSeenAt: string | null | undefined): boolean {
  if (!lastSeenAt) return false;
  return Date.now() - new Date(lastSeenAt).getTime() < 2 * 60_000;
}

/* ───────────── Skeleton ───────────── */

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('skeleton', className)} aria-hidden="true" />;
}

/* ───────────── EmptyState ───────────── */

export function EmptyState({
  icon, title, description, action, className,
}: {
  icon?: React.ReactNode;
  title: string;
  description?: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn(
      'flex flex-col items-center justify-center text-center px-6 py-14 rounded-xl border border-dashed border-surface-border bg-surface-raised/60',
      className,
    )}>
      {icon && (
        <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-brand-600">
          {icon}
        </div>
      )}
      <p className="font-semibold text-ink-900">{title}</p>
      {description && <p className="mt-1 max-w-sm text-sm text-ink-500">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

/* ───────────── relativeTime util ───────────── */

export function relativeTime(iso: string): string {
  const then = new Date(iso).getTime();
  const diff = Date.now() - then;
  const m = Math.floor(diff / 60_000);
  if (m < 1)   return 'şimdi';
  if (m < 60)  return `${m} dk önce`;
  const h = Math.floor(m / 60);
  if (h < 24)  return `${h} sa önce`;
  const d = Math.floor(h / 24);
  if (d < 7)   return `${d} gün önce`;
  return new Date(iso).toLocaleDateString('tr-TR');
}
