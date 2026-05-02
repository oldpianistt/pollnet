import { useEffect, useState, type FormEvent } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../store/auth';
import { authApi, messagingApi, userApi } from '../api/endpoints';
import { Avatar, Button } from './ui';
import { cn } from '../lib/cn';
import { NotificationsBell } from '../features/notifications/NotificationsBell';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8090';
function fullAvatarUrl(u: string | null | undefined): string | null {
  if (!u) return null;
  return u.startsWith('http') ? u : `${baseURL}${u}`;
}

export function Layout() {
  const { user, refreshToken, clear } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  // Close drawer on route change.
  useEffect(() => { setMenuOpen(false); }, [location.pathname]);

  // Heartbeat — pings the backend every 30s while the tab is open so the
  // user appears "online" to others. Only fires when authenticated.
  useEffect(() => {
    if (!user) return;
    const tick = () => userApi.heartbeat().catch(() => { /* silent */ });
    tick();
    const id = setInterval(tick, 30_000);
    return () => clearInterval(id);
  }, [user]);

  // Total unread DM count for the navbar badge.
  const dmUnread = useQuery<number>({
    queryKey: ['dm-unread'],
    queryFn:  messagingApi.unreadCount,
    enabled:  !!user,
    refetchInterval: 15_000,
  });

  async function handleLogout() {
    setMenuOpen(false);
    if (refreshToken) {
      try { await authApi.logout(refreshToken); } catch { /* best-effort */ }
    }
    clear();
    navigate('/login');
  }

  function submitSearch(e: FormEvent) {
    e.preventDefault();
    const q = searchTerm.trim();
    if (q.length < 2) return;
    setMenuOpen(false);
    navigate(`/search?q=${encodeURIComponent(q)}`);
  }

  return (
    <div className="min-h-screen bg-surface-base text-ink-700">
      <header className="sticky top-0 z-20 border-b border-surface-border bg-surface-raised/80 backdrop-blur-md supports-[backdrop-filter]:bg-surface-raised/70">
        <div className="mx-auto flex h-16 max-w-5xl items-center gap-3 px-4">
          <Link to="/" className="group flex items-center gap-2 whitespace-nowrap">
            <span className="grid h-8 w-8 place-items-center rounded-lg gradient-brand text-white shadow-sm group-hover:scale-105 transition-transform duration-150">
              <svg className="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
                <path d="M4 4v16M4 6h11a4 4 0 0 1 0 8H4M4 14h13a4 4 0 0 1 0 8H4"/>
              </svg>
            </span>
            <span className="text-lg font-semibold tracking-tightish text-ink-900">PollNet</span>
          </Link>

          {user && (
            <form onSubmit={submitSearch} className="hidden md:flex flex-1 max-w-sm ml-4">
              <div className="relative w-full">
                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
                <input
                  type="search"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Ara…"
                  className="h-10 w-full rounded-lg border border-surface-border bg-surface-subtle pl-9 pr-3 text-sm text-ink-900 placeholder:text-ink-400 transition-all duration-150 hover:border-surface-border-strong focus:bg-surface-raised focus:outline-none focus:border-brand-500 focus:shadow-ring"
                />
              </div>
            </form>
          )}

          <div className="ml-auto flex items-center gap-1">
            {user ? (
              <>
                <nav className="hidden md:flex items-center gap-1">
                  <NavTab to="/">Akış</NavTab>
                  <NavTab to="/polls/new">Oluştur</NavTab>
                  <NavTab to="/alice" sparkle ai>Alice</NavTab>
                  <NavTab to="/messages" badge={dmUnread.data ?? 0}>Mesajlar</NavTab>
                  <NavTab to="/invitations">Davetler</NavTab>
                  <NotificationsBell />
                  <NavLink
                    to={`/u/${user.username}`}
                    className={({ isActive }) =>
                      cn(
                        'ml-2 inline-flex items-center gap-2 rounded-full pl-1 pr-3 py-1 text-sm font-medium transition-all duration-150',
                        isActive ? 'bg-surface-subtle ring-1 ring-surface-border' : 'hover:bg-surface-subtle',
                      )
                    }
                  >
                    <Avatar name={user.displayName ?? user.username} src={fullAvatarUrl(user.avatarUrl)} size="sm" />
                    <span className="text-ink-900">{user.displayName ?? user.username}</span>
                  </NavLink>
                  <Button variant="ghost" size="sm" onClick={handleLogout}>Çıkış</Button>
                </nav>

                <div className="md:hidden flex items-center gap-1">
                  <NotificationsBell />
                  <button
                    type="button"
                    aria-label="Menü"
                    aria-expanded={menuOpen}
                    onClick={() => setMenuOpen((v) => !v)}
                    className="inline-flex h-10 w-10 items-center justify-center rounded-lg text-ink-700 hover:bg-surface-subtle transition-colors"
                  >
                    {menuOpen ? <CloseIcon /> : <MenuIcon />}
                  </button>
                </div>
              </>
            ) : (
              <nav className="flex items-center gap-1 text-sm">
                <Link to="/login" className="rounded-lg px-3 py-2 font-medium text-ink-700 hover:bg-surface-subtle transition-colors">Giriş</Link>
                <Link to="/register" className="rounded-lg gradient-brand px-3 py-2 font-medium text-white shadow-sm hover:brightness-105 transition-all">Kayıt ol</Link>
              </nav>
            )}
          </div>
        </div>

        {user && menuOpen && (
          <div className="md:hidden border-t border-surface-border bg-surface-raised px-4 py-3 space-y-2 animate-fade-in">
            <form onSubmit={submitSearch}>
              <div className="relative">
                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-ink-400" />
                <input
                  type="search"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  placeholder="Ara…"
                  className="h-11 w-full rounded-lg border border-surface-border bg-surface-subtle pl-9 pr-3 text-sm focus:bg-surface-raised focus:outline-none focus:border-brand-500 focus:shadow-ring"
                />
              </div>
            </form>
            <DrawerLink to="/">Akış</DrawerLink>
            <DrawerLink to="/polls/new">Anket oluştur</DrawerLink>
            <DrawerLink to="/alice" sparkle ai>Alice</DrawerLink>
            <DrawerLink to="/messages" badge={dmUnread.data ?? 0}>Mesajlar</DrawerLink>
            <DrawerLink to="/invitations">Davetler</DrawerLink>
            <DrawerLink to={`/u/${user.username}`}>{user.displayName ?? user.username}</DrawerLink>
            <button
              type="button"
              onClick={handleLogout}
              className="block w-full rounded-lg px-3 py-2 text-left text-sm font-medium text-red-600 hover:bg-red-50 transition-colors"
            >
              Çıkış
            </button>
          </div>
        )}
      </header>

      <main className="mx-auto max-w-3xl px-4 py-8 animate-fade-up">
        <Outlet />
      </main>
    </div>
  );
}

function NavTab({
  to, children, badge, sparkle, ai,
}: { to: string; children: React.ReactNode; badge?: number; sparkle?: boolean; ai?: boolean }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        cn(
          'relative inline-flex items-center gap-1 whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium transition-all duration-150',
          isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-700 hover:bg-surface-subtle hover:text-ink-900',
        )
      }
    >
      {sparkle && <span aria-hidden>✨</span>}
      {children}
      {ai && (
        <span className="ml-1 inline-flex h-4 items-center rounded-full bg-brand-600 px-1.5 text-[9px] font-bold uppercase tracking-wide text-white">
          AI
        </span>
      )}
      {badge !== undefined && badge > 0 && (
        <span className="ml-1.5 inline-flex h-4 min-w-[1rem] items-center justify-center rounded-full bg-red-600 px-1 text-[10px] font-semibold text-white">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </NavLink>
  );
}

function DrawerLink({
  to, children, badge, sparkle, ai,
}: { to: string; children: React.ReactNode; badge?: number; sparkle?: boolean; ai?: boolean }) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        cn(
          'flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
          isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-700 hover:bg-surface-subtle',
        )
      }
    >
      {sparkle && <span aria-hidden>✨</span>}
      <span>{children}</span>
      {ai && (
        <span className="inline-flex h-4 items-center rounded-full bg-brand-600 px-1.5 text-[9px] font-bold uppercase tracking-wide text-white">
          AI
        </span>
      )}
      {badge !== undefined && badge > 0 && (
        <span className="ml-auto inline-flex h-5 min-w-[1.25rem] items-center justify-center rounded-full bg-red-600 px-1.5 text-[11px] font-semibold text-white">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </NavLink>
  );
}

function MenuIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <line x1="4" y1="7" x2="20" y2="7"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="17" x2="20" y2="17"/>
    </svg>
  );
}
function CloseIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/>
    </svg>
  );
}
function SearchIcon({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>
    </svg>
  );
}
