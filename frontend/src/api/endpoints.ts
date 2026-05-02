import { api } from './client';
import type {
  AlicePollDraft,
  AuthResponse,
  CommentView,
  ConversationView,
  CursorPage,
  InvitationView,
  MeView,
  MessageView,
  NotificationView,
  PollView,
  PublicProfile,
  QuestionType,
  QuotaView,
  ResultsView,
  UUID,
  UserSummary,
} from '../lib/types';

export const authApi = {
  register: (body: {
    inviteToken: string;
    username: string;
    email: string;
    password: string;
    displayName?: string;
  }) => api.post<AuthResponse>('/api/auth/register', body).then((r) => r.data),

  login: (body: { usernameOrEmail: string; password: string }) =>
    api.post<AuthResponse>('/api/auth/login', body).then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post('/api/auth/logout', { refreshToken }).then((r) => r.data),

  verifyEmail: (token: string) =>
    api.post<void>('/api/auth/verify-email', { token }).then((r) => r.data),

  forgotPassword: (email: string) =>
    api.post<void>('/api/auth/forgot-password', { email }).then((r) => r.data),

  resetPassword: (token: string, newPassword: string) =>
    api.post<void>('/api/auth/reset-password', { token, newPassword }).then((r) => r.data),
};

export const notificationApi = {
  list: (cursor?: string) =>
    api.get<CursorPage<NotificationView>>('/api/notifications', { params: { cursor } })
       .then((r) => r.data),

  unreadCount: () =>
    api.get<{ count: number }>('/api/notifications/unread-count').then((r) => r.data.count),

  markRead: (id: UUID) =>
    api.patch<void>(`/api/notifications/${id}/read`).then((r) => r.data),

  markAllRead: () =>
    api.patch<{ updated: number }>('/api/notifications/read-all').then((r) => r.data),
};

export const searchApi = {
  polls: (q: string, cursor?: string) =>
    api.get<CursorPage<PollView>>('/api/search/polls', { params: { q, cursor } })
       .then((r) => r.data),

  users: (q: string) =>
    api.get<UserSummary[]>('/api/search/users', { params: { q } }).then((r) => r.data),
};

export const userApi = {
  me: () => api.get<MeView>('/api/users/me').then((r) => r.data),
  updateMe: (body: { displayName?: string; bio?: string }) =>
    api.patch<MeView>('/api/users/me', body).then((r) => r.data),
  profile: (username: string) =>
    api.get<PublicProfile>(`/api/users/${username}`).then((r) => r.data),
  userPolls: (username: string) =>
    api.get<PollView[]>(`/api/users/${username}/polls`).then((r) => r.data),
  follow: (username: string) =>
    api.post<void>(`/api/users/${username}/follow`).then((r) => r.data),
  unfollow: (username: string) =>
    api.delete<void>(`/api/users/${username}/follow`).then((r) => r.data),
  followers: (username: string) =>
    api.get<UserSummary[]>(`/api/users/${username}/followers`).then((r) => r.data),
  following: (username: string) =>
    api.get<UserSummary[]>(`/api/users/${username}/following`).then((r) => r.data),
  uploadAvatar: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<MeView>('/api/users/me/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data);
  },
  removeAvatar: () => api.delete<void>('/api/users/me/avatar').then((r) => r.data),
  heartbeat:    () => api.post<void>('/api/users/me/heartbeat').then((r) => r.data),
};

export const messagingApi = {
  conversations: () =>
    api.get<ConversationView[]>('/api/messages/conversations').then((r) => r.data),

  open: (peerUsername: string) =>
    api.post<ConversationView>('/api/messages/conversations', { peerUsername }).then((r) => r.data),

  unreadCount: () =>
    api.get<{ count: number }>('/api/messages/unread-count').then((r) => r.data.count),

  messages: (convoId: UUID, cursor?: string) =>
    api.get<CursorPage<MessageView>>(`/api/messages/conversations/${convoId}/messages`, { params: { cursor } })
       .then((r) => r.data),

  send: (convoId: UUID, body: string, attachmentUrl?: string | null) =>
    api.post<MessageView>(`/api/messages/conversations/${convoId}/messages`,
                          { body, attachmentUrl }).then((r) => r.data),

  markRead: (convoId: UUID) =>
    api.post<{ updated: number }>(`/api/messages/conversations/${convoId}/read`).then((r) => r.data),

  uploadAttachment: (file: File) => {
    const fd = new FormData();
    fd.append('file', file);
    return api.post<{ url: string }>('/api/messages/attachments', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data.url);
  },
};

export const pollApi = {
  get: (id: UUID) => api.get<PollView>(`/api/polls/${id}`).then((r) => r.data),

  create: (body: {
    title: string;
    description?: string;
    resultsVisibility?: 'AFTER_VOTE' | 'ALWAYS' | 'AUTHOR_ONLY';
    openAnswersVisibility?: 'PUBLIC' | 'AUTHOR_ONLY';
    questions: Array<{ type: QuestionType; prompt: string; payload: Record<string, unknown> }>;
  }) => api.post<PollView>('/api/polls', body).then((r) => r.data),

  delete: (id: UUID) => api.delete<void>(`/api/polls/${id}`).then((r) => r.data),

  submitAnswers: (
    pollId: UUID,
    answers: Array<{ questionId: UUID; payload: Record<string, unknown> }>,
  ) => api.post<void>(`/api/polls/${pollId}/answers`, { answers }).then((r) => r.data),

  results: (pollId: UUID) =>
    api.get<ResultsView>(`/api/polls/${pollId}/results`).then((r) => r.data),
};

export const feedApi = {
  feed: (type: 'discover' | 'following', cursor?: string, limit = 20) =>
    api
      .get<CursorPage<PollView>>('/api/feed', { params: { type, cursor, limit } })
      .then((r) => r.data),
};

export const commentApi = {
  list: (pollId: UUID, cursor?: string) =>
    api
      .get<CursorPage<CommentView>>(`/api/polls/${pollId}/comments`, { params: { cursor } })
      .then((r) => r.data),

  create: (pollId: UUID, body: string) =>
    api.post<CommentView>(`/api/polls/${pollId}/comments`, { body }).then((r) => r.data),

  delete: (commentId: UUID) =>
    api.delete<void>(`/api/comments/${commentId}`).then((r) => r.data),
};

export const aliceApi = {
  status: () => api.get<{ enabled: boolean }>('/api/alice/status').then((r) => r.data),

  suggestPoll: (prompt: string) =>
    api.post<AlicePollDraft>('/api/alice/suggest-poll', { prompt }).then((r) => r.data),
};

export const invitationApi = {
  myInvitations: () => api.get<InvitationView[]>('/api/invitations').then((r) => r.data),
  create: () => api.post<InvitationView>('/api/invitations').then((r) => r.data),
  quota: () => api.get<QuotaView>('/api/invitations/quota').then((r) => r.data),
};
