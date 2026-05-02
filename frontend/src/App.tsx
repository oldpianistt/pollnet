import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Spinner } from './components/ui';

// Auth pages — small, eagerly loaded so the first navigation is instant.
import { LoginPage } from './features/auth/LoginPage';
import { RegisterPage } from './features/auth/RegisterPage';
import { ForgotPasswordPage } from './features/auth/ForgotPasswordPage';
import { ResetPasswordPage } from './features/auth/ResetPasswordPage';
import { VerifyEmailPage } from './features/auth/VerifyEmailPage';

// Heavier authenticated pages — code-split to keep the initial bundle lean.
// PollDetailPage / FeedPage pull Recharts (~150 kB), PollCreatePage is form-heavy.
const FeedPage         = lazy(() => import('./features/feed/FeedPage')             .then((m) => ({ default: m.FeedPage })));
const PollDetailPage   = lazy(() => import('./features/poll/PollDetailPage')       .then((m) => ({ default: m.PollDetailPage })));
const PollCreatePage   = lazy(() => import('./features/poll/PollCreatePage')       .then((m) => ({ default: m.PollCreatePage })));
const ProfilePage      = lazy(() => import('./features/profile/ProfilePage')       .then((m) => ({ default: m.ProfilePage })));
const EditProfilePage  = lazy(() => import('./features/profile/EditProfilePage')   .then((m) => ({ default: m.EditProfilePage })));
const InvitationsPage  = lazy(() => import('./features/invitations/InvitationsPage').then((m) => ({ default: m.InvitationsPage })));
const SearchPage       = lazy(() => import('./features/search/SearchPage')         .then((m) => ({ default: m.SearchPage })));
const MessagesPage     = lazy(() => import('./features/messages/MessagesPage')     .then((m) => ({ default: m.MessagesPage })));
const AlicePage        = lazy(() => import('./features/alice/AlicePage')           .then((m) => ({ default: m.AlicePage })));

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, refetchOnWindowFocus: false, retry: 1 },
  },
});

function PageLoader() {
  return <div className="flex justify-center py-16"><Spinner /></div>;
}

function lazyProtected(Page: React.ComponentType): React.ReactElement {
  return (
    <ProtectedRoute>
      <Suspense fallback={<PageLoader />}>
        <Page />
      </Suspense>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            {/* Public */}
            <Route path="/login"           element={<LoginPage />} />
            <Route path="/register"        element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/reset-password"  element={<ResetPasswordPage />} />
            <Route path="/verify-email"    element={<VerifyEmailPage />} />

            {/* Protected (lazy chunks) */}
            <Route index                    element={lazyProtected(FeedPage)} />
            <Route path="/polls/new"        element={lazyProtected(PollCreatePage)} />
            <Route path="/polls/:id"        element={lazyProtected(PollDetailPage)} />
            <Route path="/u/:username"      element={lazyProtected(ProfilePage)} />
            <Route path="/settings/profile" element={lazyProtected(EditProfilePage)} />
            <Route path="/invitations"      element={lazyProtected(InvitationsPage)} />
            <Route path="/search"           element={lazyProtected(SearchPage)} />
            <Route path="/messages"         element={lazyProtected(MessagesPage)} />
            <Route path="/messages/:id"     element={lazyProtected(MessagesPage)} />
            <Route path="/alice"            element={lazyProtected(AlicePage)} />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
