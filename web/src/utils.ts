import type { ChatSession, LocalChatMessage, SourceReference } from "./types";

export const QUICK_PROMPTS = [
  "总结已上传文档中的核心观点",
  "对比不同文档里的关键结论",
  "基于资料生成一份项目周报",
  "提取文档中的时间线和责任人"
];

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

  let answerMatched = false;
  return [...session.messages]
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .map<LocalChatMessage>((message) => {
      const attachSources =
        !answerMatched &&
        latestAnswer &&
        message.role.toLowerCase() === "assistant" &&
        message.content === latestAnswer.answer;

      if (attachSources) {
        answerMatched = true;
      }

      return {
        ...message,
        sources: attachSources ? latestAnswer.sources : undefined
      };
    });
}
