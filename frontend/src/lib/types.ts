// Mirrors the backend record shapes. Kept in one file so cross-feature imports stay short.

export type UUID = string;

export type QuestionType = 'SINGLE' | 'MULTIPLE' | 'LIKERT' | 'RANKING' | 'OPEN';
export type ResultsVisibility = 'AFTER_VOTE' | 'ALWAYS' | 'AUTHOR_ONLY';
export type OpenAnswersVisibility = 'PUBLIC' | 'AUTHOR_ONLY';
export type NotificationType = 'NEW_FOLLOWER' | 'POLL_ANSWERED' | 'POLL_COMMENTED';

export interface UserSummary {
  id: UUID;
  username: string;
  displayName: string | null;
  avatarUrl: string | null;
  lastSeenAt: string | null;
}

export interface MeView {
  id: UUID;
  username: string;
  email: string;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  inviteQuota: number;
  createdAt: string;
}

export interface PublicProfile {
  id: UUID;
  username: string;
  displayName: string | null;
  bio: string | null;
  avatarUrl: string | null;
  createdAt: string;
  lastSeenAt: string | null;
  followerCount: number;
  followingCount: number;
  viewerFollows: boolean;
}

export interface ConversationView {
  id: UUID;
  peer: UserSummary;
  lastMessagePreview: string | null;
  lastMessageAt: string;
  unreadCount: number;
}

export interface MessageView {
  id: UUID;
  sender: UserSummary;
  body: string;
  attachmentUrl: string | null;
  createdAt: string;
  readAt: string | null;
}

export interface QuestionView {
  id: UUID;
  type: QuestionType;
  prompt: string;
  payload: Record<string, unknown>;
  position: number;
}

export interface PollView {
  id: UUID;
  author: UserSummary;
  title: string;
  description: string | null;
  resultsVisibility: ResultsVisibility;
  openAnswersVisibility: OpenAnswersVisibility;
  questions: QuestionView[];
  createdAt: string;
  viewerHasAnswered: boolean;
}

export interface ResultsView {
  pollId: UUID;
  results: Array<{
    questionId: UUID;
    type: QuestionType;
    prompt: string;
    data: Record<string, unknown>;
  }>;
}

export interface CommentView {
  id: UUID;
  author: UserSummary;
  body: string;
  createdAt: string;
}

export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

export interface InvitationView {
  id: UUID;
  token: string;
  expiresAt: string;
  createdAt: string;
}

export interface QuotaView {
  remaining: number;
  monthlyAllowance: number;
  resetAt: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  accessTtlSeconds: number;
  user: { id: UUID; username: string; email: string; displayName: string | null; avatarUrl?: string | null };
}

export interface NotificationView {
  id: UUID;
  type: NotificationType;
  payload: Record<string, unknown>;
  createdAt: string;
  readAt: string | null;
}

// Alice — poll-suggestion module. Returned by POST /api/alice/suggest-poll.
// Loose shape on purpose: payload fields differ per question type and we
// validate again when seeding the create form.
export interface AlicePollDraft {
  title: string;
  description: string | null;
  questions: Array<{
    type: QuestionType;
    prompt: string;
    payload: {
      options?: string[];
      minSelect?: number;
      maxSelect?: number;
      scale?: number;
      leftLabel?: string;
      rightLabel?: string;
      maxLength?: number;
    };
  }>;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path?: string;
  errors?: Array<{ field: string; message: string }>;
}
