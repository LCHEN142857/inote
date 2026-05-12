import type { ChatSession, LocalChatMessage, SourceReference } from "./types";

export const PINNED_SESSIONS_KEY = "inote-pinned-sessions";
export const ACTIVE_DOCUMENT_STATUSES = new Set(["PENDING", "PARSING", "PROCESSING"]);

export function formatTime(value?: string) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

export function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function getStoredPinnedSessions() {
  try {
    const raw = window.localStorage.getItem(PINNED_SESSIONS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === "string") : [];
  } catch {
    return [];
  }
}

export function buildMessages(
  session: ChatSession | null,
  latestAnswer?: { answer: string; sources: SourceReference[] }
) {
  if (!session) return [];

  return [...session.messages]
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .map<LocalChatMessage>((message) => {
      return {
        ...message,
        sources:
          message.sources ??
          (latestAnswer &&
          message.role.toLowerCase() === "assistant" &&
          message.content === latestAnswer.answer
            ? latestAnswer.sources
            : undefined)
      };
    });
}
