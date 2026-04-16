// 说明当前这行代码的作用。
import { useEffect, useMemo, useRef, useState } from "react";
// 说明当前这行代码的作用。
import { api } from "./api";
// 说明当前这行代码的作用。
import type {
  // 说明当前这行代码的作用。
  AuthCaptcha,
  // 说明当前这行代码的作用。
  AuthResponse,
  // 说明当前这行代码的作用。
  ChatSession,
  // 说明当前这行代码的作用。
  ChatSessionSummary,
  // 说明当前这行代码的作用。
  DocumentStatus,
  // 说明当前这行代码的作用。
  LocalChatMessage,
  // 说明当前这行代码的作用。
  SourceReference
// 说明当前这行代码的作用。
} from "./types";

// 说明当前这行代码的作用。
const QUICK_PROMPTS = [
  // 说明当前这行代码的作用。
  "总结已上传文档中的核心观点",
  // 说明当前这行代码的作用。
  "对比不同文档里的关键结论",
  // 说明当前这行代码的作用。
  "基于资料生成一份项目周报",
  // 说明当前这行代码的作用。
  "提取文档中的时间线和责任人"
// 说明当前这行代码的作用。
];

// 说明当前这行代码的作用。
const PINNED_SESSIONS_KEY = "inote-pinned-sessions";
// 说明当前这行代码的作用。
const ACTIVE_DOCUMENT_STATUSES = new Set(["PENDING", "PARSING", "PROCESSING"]);

// 说明当前这行代码的作用。
function formatTime(value?: string) {
  // 说明当前这行代码的作用。
  if (!value) return "";
  // 说明当前这行代码的作用。
  return new Intl.DateTimeFormat("zh-CN", {
    // 说明当前这行代码的作用。
    month: "short",
    // 说明当前这行代码的作用。
    day: "numeric",
    // 说明当前这行代码的作用。
    hour: "2-digit",
    // 说明当前这行代码的作用。
    minute: "2-digit"
  // 说明当前这行代码的作用。
  }).format(new Date(value));
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
function formatFileSize(bytes: number) {
  // 说明当前这行代码的作用。
  if (bytes < 1024) return `${bytes} B`;
  // 说明当前这行代码的作用。
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  // 说明当前这行代码的作用。
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
function getStoredPinnedSessions() {
  // 说明当前这行代码的作用。
  try {
    // 说明当前这行代码的作用。
    const raw = window.localStorage.getItem(PINNED_SESSIONS_KEY);
    // 说明当前这行代码的作用。
    if (!raw) return [];
    // 说明当前这行代码的作用。
    const parsed = JSON.parse(raw);
    // 说明当前这行代码的作用。
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === "string") : [];
  // 说明当前这行代码的作用。
  } catch {
    // 说明当前这行代码的作用。
    return [];
  // 说明当前这行代码的作用。
  }
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
function buildMessages(
  // 说明当前这行代码的作用。
  session: ChatSession | null,
  // 说明当前这行代码的作用。
  latestAnswer?: { answer: string; sources: SourceReference[] }
// 说明当前这行代码的作用。
) {
  // 说明当前这行代码的作用。
  if (!session) return [];

  // 说明当前这行代码的作用。
  let answerMatched = false;
  // 说明当前这行代码的作用。
  return [...session.messages]
    // 说明当前这行代码的作用。
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    // 说明当前这行代码的作用。
    .map<LocalChatMessage>((message) => {
      // 说明当前这行代码的作用。
      const attachSources =
        // 说明当前这行代码的作用。
        !answerMatched &&
        // 说明当前这行代码的作用。
        latestAnswer &&
        // 说明当前这行代码的作用。
        message.role.toLowerCase() === "assistant" &&
        // 说明当前这行代码的作用。
        message.content === latestAnswer.answer;

      // 说明当前这行代码的作用。
      if (attachSources) {
        // 说明当前这行代码的作用。
        answerMatched = true;
      // 说明当前这行代码的作用。
      }

      // 说明当前这行代码的作用。
      return {
        // 说明当前这行代码的作用。
        ...message,
        // 说明当前这行代码的作用。
        sources: attachSources ? latestAnswer.sources : undefined
      // 说明当前这行代码的作用。
      };
    // 说明当前这行代码的作用。
    });
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
function AuthView(props: {
  // 说明当前这行代码的作用。
  captcha: AuthCaptcha | null;
  // 说明当前这行代码的作用。
  loading: boolean;
  // 说明当前这行代码的作用。
  submitting: boolean;
  // 说明当前这行代码的作用。
  error: string;
  // 说明当前这行代码的作用。
  onRefresh: () => void;
  // 说明当前这行代码的作用。
  onSubmit: (payload: { username: string; password: string; captchaCode: string }) => void;
// 说明当前这行代码的作用。
}) {
  // 说明当前这行代码的作用。
  const [username, setUsername] = useState("");
  // 说明当前这行代码的作用。
  const [password, setPassword] = useState("");
  // 说明当前这行代码的作用。
  const [captchaCode, setCaptchaCode] = useState("");

  // 处理当前函数的 JSX 返回结果。
  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="eyebrow">User Access</p>
        <h1>登录或注册</h1>
        <p className="auth-copy">第一次登录自动注册，后续登录校验用户名、密码和验证码。</p>

        <label className="auth-field">
          <span>用户名</span>
          <input value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>密码</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>验证码</span>
          <div className="captcha-row">
            <input value={captchaCode} onChange={(e) => setCaptchaCode(e.target.value)} />
            <button className="captcha-badge" onClick={props.onRefresh} disabled={props.loading}>
              {props.loading ? "加载中" : props.captcha?.captchaCode ?? "刷新"}
            </button>
          </div>
        </label>

        {props.error ? <div className="error-banner auth-error">{props.error}</div> : null}

        <button
          className="primary-button"
          onClick={() => props.onSubmit({ username, password, captchaCode })}
          disabled={props.submitting || props.loading || !props.captcha}
        >
          {props.submitting ? "提交中" : "登录 / 注册"}
        </button>
      </div>
    </div>
  );
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export default function App() {
  // 说明当前这行代码的作用。
  const [authUser, setAuthUser] = useState<AuthResponse | null>(null);
  // 说明当前这行代码的作用。
  const [authLoading, setAuthLoading] = useState(true);
  // 说明当前这行代码的作用。
  const [authSubmitting, setAuthSubmitting] = useState(false);
  // 说明当前这行代码的作用。
  const [authError, setAuthError] = useState("");
  // 说明当前这行代码的作用。
  const [captcha, setCaptcha] = useState<AuthCaptcha | null>(null);

  // 说明当前这行代码的作用。
  const [sessions, setSessions] = useState<ChatSessionSummary[]>([]);
  // 说明当前这行代码的作用。
  const [documents, setDocuments] = useState<DocumentStatus[]>([]);
  // 说明当前这行代码的作用。
  const [selectedSessionId, setSelectedSessionId] = useState("");
  // 说明当前这行代码的作用。
  const [selectedSession, setSelectedSession] = useState<ChatSession | null>(null);
  // 说明当前这行代码的作用。
  const [composer, setComposer] = useState("");
  // 说明当前这行代码的作用。
  const [loading, setLoading] = useState(true);
  // 说明当前这行代码的作用。
  const [sending, setSending] = useState(false);
  // 说明当前这行代码的作用。
  const [uploading, setUploading] = useState(false);
  // 说明当前这行代码的作用。
  const [sidebarOpen, setSidebarOpen] = useState(false);
  // 说明当前这行代码的作用。
  const [error, setError] = useState("");
  // 说明当前这行代码的作用。
  const [renamingId, setRenamingId] = useState("");
  // 说明当前这行代码的作用。
  const [renameDraft, setRenameDraft] = useState("");
  // 说明当前这行代码的作用。
  const [sessionQuery, setSessionQuery] = useState("");
  // 说明当前这行代码的作用。
  const [pinnedSessionIds, setPinnedSessionIds] = useState<string[]>([]);
  // 说明当前这行代码的作用。
  const [latestAnswerMeta, setLatestAnswerMeta] = useState<{
    // 说明当前这行代码的作用。
    sessionId: string;
    // 说明当前这行代码的作用。
    answer: string;
    // 说明当前这行代码的作用。
    sources: SourceReference[];
  // 说明当前这行代码的作用。
  } | null>(null);
  // 说明当前这行代码的作用。
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  // 说明当前这行代码的作用。
  const [newPassword, setNewPassword] = useState("");
  // 说明当前这行代码的作用。
  const [confirmPassword, setConfirmPassword] = useState("");
  // 说明当前这行代码的作用。
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  // 说明当前这行代码的作用。
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  // 说明当前这行代码的作用。
  const messageEndRef = useRef<HTMLDivElement | null>(null);

  // 说明当前这行代码的作用。
  useEffect(() => {
    // 说明当前这行代码的作用。
    setPinnedSessionIds(getStoredPinnedSessions());
    // 说明当前这行代码的作用。
    void bootstrapAuth();
  // 说明当前这行代码的作用。
  }, []);

  // 说明当前这行代码的作用。
  useEffect(() => {
    // 说明当前这行代码的作用。
    window.localStorage.setItem(PINNED_SESSIONS_KEY, JSON.stringify(pinnedSessionIds));
  // 说明当前这行代码的作用。
  }, [pinnedSessionIds]);

  // 说明当前这行代码的作用。
  const activeAnswerMeta =
    // 说明当前这行代码的作用。
    latestAnswerMeta && latestAnswerMeta.sessionId === selectedSession?.id
      // 说明当前这行代码的作用。
      ? { answer: latestAnswerMeta.answer, sources: latestAnswerMeta.sources }
      // 说明当前这行代码的作用。
      : undefined;

  // 说明当前这行代码的作用。
  const messages = useMemo(
    // 说明当前这行代码的作用。
    () => buildMessages(selectedSession, activeAnswerMeta),
    // 说明当前这行代码的作用。
    [activeAnswerMeta, selectedSession]
  // 说明当前这行代码的作用。
  );

  // 说明当前这行代码的作用。
  const latestSources =
    // 说明当前这行代码的作用。
    [...messages].reverse().find((message) => message.sources?.length)?.sources ?? [];

  // 说明当前这行代码的作用。
  const completedDocuments = documents.filter((item) => item.status === "COMPLETED").length;
  // 说明当前这行代码的作用。
  const activeDocuments = documents.filter((item) =>
    // 说明当前这行代码的作用。
    ACTIVE_DOCUMENT_STATUSES.has(item.status.toUpperCase())
  // 说明当前这行代码的作用。
  ).length;

  // 说明当前这行代码的作用。
  useEffect(() => {
    // 说明当前这行代码的作用。
    messageEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  // 说明当前这行代码的作用。
  }, [messages, sending]);

  // 说明当前这行代码的作用。
  useEffect(() => {
    // 说明当前这行代码的作用。
    if (!authUser || (!activeDocuments && !uploading)) return;

    // 说明当前这行代码的作用。
    const timer = window.setInterval(() => {
      // 说明当前这行代码的作用。
      void refreshDocuments();
    // 说明当前这行代码的作用。
    }, 3000);
    // 说明当前这行代码的作用。
    return () => window.clearInterval(timer);
  // 说明当前这行代码的作用。
  }, [activeDocuments, authUser, uploading]);

  // 说明当前这行代码的作用。
  async function refreshCaptcha() {
    // 说明当前这行代码的作用。
    const nextCaptcha = await api.getCaptcha();
    // 说明当前这行代码的作用。
    setCaptcha(nextCaptcha);
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function bootstrapAuth() {
    // 说明当前这行代码的作用。
    setAuthLoading(true);
    // 说明当前这行代码的作用。
    setAuthError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      await refreshCaptcha();
      // 说明当前这行代码的作用。
      const token = window.localStorage.getItem("inote-auth-token");
      // 说明当前这行代码的作用。
      if (!token) {
        // 说明当前这行代码的作用。
        api.setToken("");
        // 说明当前这行代码的作用。
        setAuthUser(null);
        // 说明当前这行代码的作用。
        return;
      // 说明当前这行代码的作用。
      }

      // 说明当前这行代码的作用。
      api.setToken(token);
      // 说明当前这行代码的作用。
      const user = await api.me();
      // 说明当前这行代码的作用。
      setAuthUser(user);
      // 说明当前这行代码的作用。
      await bootstrapWorkspace();
    // 说明当前这行代码的作用。
    } catch {
      // 说明当前这行代码的作用。
      api.setToken("");
      // 说明当前这行代码的作用。
      setAuthUser(null);
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setAuthLoading(false);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function bootstrapWorkspace() {
    // 说明当前这行代码的作用。
    setLoading(true);
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const [sessionList, documentList] = await Promise.all([
        // 说明当前这行代码的作用。
        api.listSessions(),
        // 说明当前这行代码的作用。
        api.listDocuments()
      // 说明当前这行代码的作用。
      ]);
      // 说明当前这行代码的作用。
      setSessions(sessionList);
      // 说明当前这行代码的作用。
      setDocuments(documentList);

      // 说明当前这行代码的作用。
      const firstSessionId = sessionList[0]?.id ?? "";
      // 说明当前这行代码的作用。
      setSelectedSessionId(firstSessionId);
      // 说明当前这行代码的作用。
      if (firstSessionId) {
        // 说明当前这行代码的作用。
        setSelectedSession(await api.getSession(firstSessionId));
      // 说明当前这行代码的作用。
      } else {
        // 说明当前这行代码的作用。
        setSelectedSession(null);
      // 说明当前这行代码的作用。
      }
    // 说明当前这行代码的作用。
    } catch (bootstrapError) {
      // 说明当前这行代码的作用。
      setError(bootstrapError instanceof Error ? bootstrapError.message : "初始化失败");
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setLoading(false);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleLogin(payload: {
    // 说明当前这行代码的作用。
    username: string;
    // 说明当前这行代码的作用。
    password: string;
    // 说明当前这行代码的作用。
    captchaCode: string;
  // 说明当前这行代码的作用。
  }) {
    // 说明当前这行代码的作用。
    if (!captcha) return;
    // 说明当前这行代码的作用。
    setAuthSubmitting(true);
    // 说明当前这行代码的作用。
    setAuthError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const user = await api.login({
        // 说明当前这行代码的作用。
        username: payload.username,
        // 说明当前这行代码的作用。
        password: payload.password,
        // 说明当前这行代码的作用。
        captchaId: captcha.captchaId,
        // 说明当前这行代码的作用。
        captchaCode: payload.captchaCode
      // 说明当前这行代码的作用。
      });
      // 说明当前这行代码的作用。
      api.setToken(user.token);
      // 说明当前这行代码的作用。
      setAuthUser(user);
      // 说明当前这行代码的作用。
      await bootstrapWorkspace();
      // 说明当前这行代码的作用。
      await refreshCaptcha();
    // 说明当前这行代码的作用。
    } catch (loginError) {
      // 说明当前这行代码的作用。
      setAuthError(loginError instanceof Error ? loginError.message : "登录失败");
      // 说明当前这行代码的作用。
      await refreshCaptcha();
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setAuthSubmitting(false);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  function handleLogout() {
    // 说明当前这行代码的作用。
    api.setToken("");
    // 说明当前这行代码的作用。
    setAuthUser(null);
    // 说明当前这行代码的作用。
    setSessions([]);
    // 说明当前这行代码的作用。
    setDocuments([]);
    // 说明当前这行代码的作用。
    setSelectedSessionId("");
    // 说明当前这行代码的作用。
    setSelectedSession(null);
    // 说明当前这行代码的作用。
    setLatestAnswerMeta(null);
    // 说明当前这行代码的作用。
    setComposer("");
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    setPasswordDialogOpen(false);
    // 说明当前这行代码的作用。
    void refreshCaptcha();
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function refreshSessions(targetId?: string) {
    // 说明当前这行代码的作用。
    const sessionList = await api.listSessions();
    // 说明当前这行代码的作用。
    setSessions(sessionList);
    // 说明当前这行代码的作用。
    const nextId =
      // 说明当前这行代码的作用。
      targetId ??
      // 说明当前这行代码的作用。
      (sessionList.some((item) => item.id === selectedSessionId)
        // 说明当前这行代码的作用。
        ? selectedSessionId
        // 说明当前这行代码的作用。
        : sessionList[0]?.id ?? "");

    // 说明当前这行代码的作用。
    setSelectedSessionId(nextId);
    // 说明当前这行代码的作用。
    if (nextId) {
      // 说明当前这行代码的作用。
      setSelectedSession(await api.getSession(nextId));
    // 说明当前这行代码的作用。
    } else {
      // 说明当前这行代码的作用。
      setSelectedSession(null);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function refreshDocuments() {
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      setDocuments(await api.listDocuments());
    // 说明当前这行代码的作用。
    } catch (documentError) {
      // 说明当前这行代码的作用。
      setError(documentError instanceof Error ? documentError.message : "刷新文档失败");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleSelectSession(sessionId: string) {
    // 说明当前这行代码的作用。
    setSelectedSessionId(sessionId);
    // 说明当前这行代码的作用。
    setSidebarOpen(false);
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      setSelectedSession(await api.getSession(sessionId));
    // 说明当前这行代码的作用。
    } catch (sessionError) {
      // 说明当前这行代码的作用。
      setError(sessionError instanceof Error ? sessionError.message : "加载会话失败");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleNewSession() {
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const session = await api.createSession();
      // 说明当前这行代码的作用。
      setLatestAnswerMeta(null);
      // 说明当前这行代码的作用。
      setComposer("");
      // 说明当前这行代码的作用。
      setSidebarOpen(false);
      // 说明当前这行代码的作用。
      await refreshSessions(session.id);
    // 说明当前这行代码的作用。
    } catch (createError) {
      // 说明当前这行代码的作用。
      setError(createError instanceof Error ? createError.message : "创建会话失败");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleSend(prompt?: string) {
    // 说明当前这行代码的作用。
    const question = (prompt ?? composer).trim();
    // 说明当前这行代码的作用。
    if (!question || sending) return;

    // 说明当前这行代码的作用。
    setSending(true);
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    const now = new Date().toISOString();
    // 说明当前这行代码的作用。
    const baseSession: ChatSession = selectedSession ?? {
      // 说明当前这行代码的作用。
      id: selectedSessionId || "draft",
      // 说明当前这行代码的作用。
      title: "新会话",
      // 说明当前这行代码的作用。
      createdAt: now,
      // 说明当前这行代码的作用。
      updatedAt: now,
      // 说明当前这行代码的作用。
      messages: []
    // 说明当前这行代码的作用。
    };

    // 说明当前这行代码的作用。
    setSelectedSession({
      // 说明当前这行代码的作用。
      ...baseSession,
      // 说明当前这行代码的作用。
      messages: [
        // 说明当前这行代码的作用。
        ...baseSession.messages,
        // 说明当前这行代码的作用。
        { id: `u-${Date.now()}`, role: "user", content: question, createdAt: now },
        // 说明当前这行代码的作用。
        {
          // 说明当前这行代码的作用。
          id: `a-${Date.now()}`,
          // 说明当前这行代码的作用。
          role: "assistant",
          // 说明当前这行代码的作用。
          content: "正在检索你的知识文档并生成回答...",
          // 说明当前这行代码的作用。
          createdAt: now
        // 说明当前这行代码的作用。
        }
      // 说明当前这行代码的作用。
      ]
    // 说明当前这行代码的作用。
    });
    // 说明当前这行代码的作用。
    setComposer("");

    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const response = await api.query(selectedSessionId || undefined, question);
      // 说明当前这行代码的作用。
      setLatestAnswerMeta({
        // 说明当前这行代码的作用。
        sessionId: response.sessionId,
        // 说明当前这行代码的作用。
        answer: response.answer,
        // 说明当前这行代码的作用。
        sources: response.sources ?? []
      // 说明当前这行代码的作用。
      });
      // 说明当前这行代码的作用。
      await refreshSessions(response.sessionId);
    // 说明当前这行代码的作用。
    } catch (queryError) {
      // 说明当前这行代码的作用。
      setError(queryError instanceof Error ? queryError.message : "发送失败");
      // 说明当前这行代码的作用。
      if (selectedSessionId) {
        // 说明当前这行代码的作用。
        try {
          // 说明当前这行代码的作用。
          setSelectedSession(await api.getSession(selectedSessionId));
        // 说明当前这行代码的作用。
        } catch {
          // 说明当前这行代码的作用。
          setSelectedSession(null);
        // 说明当前这行代码的作用。
        }
      // 说明当前这行代码的作用。
      } else {
        // 说明当前这行代码的作用。
        setSelectedSession(null);
      // 说明当前这行代码的作用。
      }
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setSending(false);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleUpload(files: FileList | null) {
    // 说明当前这行代码的作用。
    if (!files?.length || uploading) return;
    // 说明当前这行代码的作用。
    setUploading(true);
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      await Promise.all(Array.from(files).map((file) => api.uploadDocument(file)));
      // 说明当前这行代码的作用。
      await refreshDocuments();
    // 说明当前这行代码的作用。
    } catch (uploadError) {
      // 说明当前这行代码的作用。
      setError(uploadError instanceof Error ? uploadError.message : "上传失败");
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setUploading(false);
      // 说明当前这行代码的作用。
      if (fileInputRef.current) {
        // 说明当前这行代码的作用。
        fileInputRef.current.value = "";
      // 说明当前这行代码的作用。
      }
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleRenameSubmit(sessionId: string) {
    // 说明当前这行代码的作用。
    const title = renameDraft.trim();
    // 说明当前这行代码的作用。
    if (!title) return;
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const updated = await api.updateSession(sessionId, title);
      // 说明当前这行代码的作用。
      setRenamingId("");
      // 说明当前这行代码的作用。
      setRenameDraft("");
      // 说明当前这行代码的作用。
      await refreshSessions(updated.id);
    // 说明当前这行代码的作用。
    } catch (renameError) {
      // 说明当前这行代码的作用。
      setError(renameError instanceof Error ? renameError.message : "重命名失败");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleDeleteSession(sessionId: string) {
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      await api.deleteSession(sessionId);
      // 说明当前这行代码的作用。
      setPinnedSessionIds((current) => current.filter((item) => item !== sessionId));
      // 说明当前这行代码的作用。
      if (latestAnswerMeta?.sessionId === sessionId) {
        // 说明当前这行代码的作用。
        setLatestAnswerMeta(null);
      // 说明当前这行代码的作用。
      }
      // 说明当前这行代码的作用。
      await refreshSessions();
    // 说明当前这行代码的作用。
    } catch (deleteError) {
      // 说明当前这行代码的作用。
      setError(deleteError instanceof Error ? deleteError.message : "删除失败");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  async function handleResetPassword() {
    // 说明当前这行代码的作用。
    setPasswordSubmitting(true);
    // 说明当前这行代码的作用。
    setError("");
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const response = await api.resetPassword(newPassword, confirmPassword);
      // 说明当前这行代码的作用。
      setNewPassword("");
      // 说明当前这行代码的作用。
      setConfirmPassword("");
      // 说明当前这行代码的作用。
      setPasswordDialogOpen(false);
      // 说明当前这行代码的作用。
      setError(response.message);
    // 说明当前这行代码的作用。
    } catch (passwordError) {
      // 说明当前这行代码的作用。
      setError(passwordError instanceof Error ? passwordError.message : "密码重置失败");
    // 说明当前这行代码的作用。
    } finally {
      // 说明当前这行代码的作用。
      setPasswordSubmitting(false);
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  function togglePinSession(sessionId: string) {
    // 说明当前这行代码的作用。
    setPinnedSessionIds((current) =>
      // 说明当前这行代码的作用。
      current.includes(sessionId)
        // 说明当前这行代码的作用。
        ? current.filter((item) => item !== sessionId)
        // 说明当前这行代码的作用。
        : [sessionId, ...current]
    // 说明当前这行代码的作用。
    );
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  const filteredSessions = useMemo(() => {
    // 说明当前这行代码的作用。
    const normalized = sessionQuery.trim().toLowerCase();
    // 说明当前这行代码的作用。
    const filtered = normalized
      // 说明当前这行代码的作用。
      ? sessions.filter((session) => session.title.toLowerCase().includes(normalized))
      // 说明当前这行代码的作用。
      : sessions;

    // 说明当前这行代码的作用。
    return filtered.slice().sort((left, right) => {
      // 说明当前这行代码的作用。
      const leftPinned = pinnedSessionIds.includes(left.id) ? 1 : 0;
      // 说明当前这行代码的作用。
      const rightPinned = pinnedSessionIds.includes(right.id) ? 1 : 0;
      // 说明当前这行代码的作用。
      if (leftPinned !== rightPinned) return rightPinned - leftPinned;
      // 说明当前这行代码的作用。
      return new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime();
    // 说明当前这行代码的作用。
    });
  // 说明当前这行代码的作用。
  }, [pinnedSessionIds, sessionQuery, sessions]);

  // 说明当前这行代码的作用。
  if (!authUser) {
    // 处理当前函数的 JSX 返回结果。
    return (
      <AuthView
        captcha={captcha}
        loading={authLoading}
        submitting={authSubmitting}
        error={authError}
        onRefresh={() => void refreshCaptcha()}
        onSubmit={(payload) => void handleLogin(payload)}
      />
    );
  // 说明当前这行代码的作用。
  }

  // 处理当前函数的 JSX 返回结果。
  return (
    <div className="app-shell">
      <aside className={`sidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="brand-card">
          <div className="brand-mark">i</div>
          <div>
            <p className="eyebrow">Knowledge Copilot</p>
            <h1>inote</h1>
          </div>
        </div>

        <div className="user-badge">
          <strong>{authUser.username}</strong>
          <span>只访问自己的知识和会话</span>
        </div>

        <button className="primary-button" onClick={() => void handleNewSession()}>
          新建对话
        </button>

        <div className="search-box">
          <input
            value={sessionQuery}
            onChange={(event) => setSessionQuery(event.target.value)}
            placeholder="搜索会话标题"
          />
        </div>

        <div className="panel-header">
          <span>会话列表</span>
          <span>{filteredSessions.length}</span>
        </div>

        <div className="session-list">
          {filteredSessions.length === 0 ? (
            <div className="empty-tile compact">
              {sessionQuery ? "没有匹配的会话。" : "还没有会话，先发起第一轮问答。"}
            </div>
          ) : (
            filteredSessions.map((session) => {
              const pinned = pinnedSessionIds.includes(session.id);
              return (
                <article
                  key={session.id}
                  className={`session-card ${session.id === selectedSessionId ? "active" : ""}`}
                >
                  {renamingId === session.id ? (
                    <>
                      <input
                        className="ghost-input"
                        value={renameDraft}
                        onChange={(event) => setRenameDraft(event.target.value)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter") void handleRenameSubmit(session.id);
                          if (event.key === "Escape") {
                            setRenamingId("");
                            setRenameDraft("");
                          }
                        }}
                        autoFocus
                      />
                      <div className="session-actions">
                        <button onClick={() => void handleRenameSubmit(session.id)}>保存</button>
                        <button onClick={() => { setRenamingId(""); setRenameDraft(""); }}>
                          取消
                        </button>
                      </div>
                    </>
                  ) : (
                    <>
                      <button
                        className="session-main"
                        onClick={() => void handleSelectSession(session.id)}
                      >
                        <div className="session-title-row">
                          <strong>{session.title || "未命名会话"}</strong>
                          {pinned ? <span className="pin-badge">已固定</span> : null}
                        </div>
                        <span>{session.messageCount} 条消息</span>
                        <time>{formatTime(session.updatedAt)}</time>
                      </button>
                      <div className="session-actions">
                        <button onClick={() => togglePinSession(session.id)}>
                          {pinned ? "取消固定" : "固定"}
                        </button>
                        <button onClick={() => { setRenamingId(session.id); setRenameDraft(session.title); }}>
                          重命名
                        </button>
                        <button onClick={() => void handleDeleteSession(session.id)}>删除</button>
                      </div>
                    </>
                  )}
                </article>
              );
            })
          )}
        </div>
      </aside>

      <main className="main-panel">
        <div className="topbar">
          <button className="icon-button mobile-only" onClick={() => setSidebarOpen((v) => !v)}>
            菜单
          </button>
          <div>
            <p className="eyebrow">RAG Workspace</p>
            <h2>{selectedSession?.title || "与你的知识库开始对话"}</h2>
          </div>
          <div className="topbar-status">
            <span>{completedDocuments} 已入库</span>
            <span>{sessions.length} 会话</span>
            {activeDocuments ? <span>{activeDocuments} 文档处理中</span> : null}
            <button className="text-button" onClick={() => setPasswordDialogOpen(true)}>忘记密码</button>
            <button className="text-button" onClick={handleLogout}>退出登录</button>
          </div>
        </div>

        {error ? <div className="error-banner">{error}</div> : null}

        {passwordDialogOpen ? (
          <section className="password-card">
            <div className="panel-header">
              <span>重置密码</span>
              <button className="text-button" onClick={() => setPasswordDialogOpen(false)}>
                关闭
              </button>
            </div>
            <div className="password-grid">
              <input
                type="password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                placeholder="输入新密码"
              />
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                placeholder="确认新密码"
              />
              <button
                className="primary-button"
                onClick={() => void handleResetPassword()}
                disabled={passwordSubmitting}
              >
                {passwordSubmitting ? "提交中" : "提交"}
              </button>
            </div>
          </section>
        ) : null}

        <section className="chat-panel">
          {loading ? (
            <div className="empty-state">正在加载数据...</div>
          ) : messages.length === 0 ? (
            <div className="hero-card">
              <div className="hero-copy">
                <p className="eyebrow">Inote AI Workspace</p>
                <h3>把文档、知识与问答放进同一个工作台</h3>
                <p>
                  已登录用户只能查看自己的文档和会话。上传文件后，直接围绕资料提问，
                  回答会附带来源引用。
                </p>
              </div>
              <div className="prompt-grid">
                {QUICK_PROMPTS.map((prompt) => (
                  <button key={prompt} className="prompt-card" onClick={() => void handleSend(prompt)}>
                    {prompt}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className="message-stream">
              {messages.map((message) => (
                <article
                  key={message.id}
                  className={`message ${message.role.toLowerCase()} ${message.pending ? "pending" : ""}`}
                >
                  <div className="message-label">
                    {message.role.toLowerCase() === "user" ? authUser.username : "inote"}
                    <time>{formatTime(message.createdAt)}</time>
                  </div>
                  <div className="message-body plain-body">{message.content}</div>
                  {message.sources?.length ? (
                    <div className="inline-sources">
                      {message.sources.map((source) => (
                        <a key={`${source.fileName}-${source.url}`} href={source.url} target="_blank" rel="noreferrer">
                          {source.fileName}
                        </a>
                      ))}
                    </div>
                  ) : null}
                </article>
              ))}
              <div ref={messageEndRef} />
            </div>
          )}
        </section>

        <div className="composer-panel">
          <textarea
            value={composer}
            onChange={(event) => setComposer(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                void handleSend();
              }
            }}
            placeholder="输入你的问题，例如：这批文档里有哪些关键风险与行动项？"
            rows={1}
          />
          <div className="composer-actions">
            <span>Enter 发送，Shift + Enter 换行</span>
            <button className="primary-button" onClick={() => void handleSend()} disabled={sending}>
              {sending ? "生成中" : "发送"}
            </button>
          </div>
        </div>
      </main>

      <aside className="knowledge-panel">
        <section className="knowledge-card upload-card">
          <div className="panel-header">
            <span>知识库</span>
            <span>{documents.length}</span>
          </div>
          <p className="panel-copy">支持 PDF、Word、Excel、TXT、CSV。</p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            hidden
            onChange={(event) => void handleUpload(event.target.files)}
          />
          <button className="primary-button" onClick={() => fileInputRef.current?.click()} disabled={uploading}>
            {uploading ? "上传中" : "上传文件"}
          </button>
          {activeDocuments ? <p className="panel-tip">检测到文档仍在处理，列表会自动轮询刷新。</p> : null}
        </section>
        <section className="knowledge-card">
          <div className="panel-header">
            <span>文档状态</span>
            <button className="text-button" onClick={() => void refreshDocuments()}>
              刷新
            </button>
          </div>
          <div className="document-list">
            {documents.length === 0 ? (
              <div className="empty-tile compact">还没有文档。</div>
            ) : (
              documents
                .slice()
                .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
                .map((document) => (
                  <article key={document.documentId} className="document-card">
                    <div className="document-title-row">
                      <strong>{document.fileName}</strong>
                      <span className={`status-pill status-${document.status.toLowerCase()}`}>
                        {document.status}
                      </span>
                    </div>
                    <div className="document-meta">
                      <span>{formatFileSize(document.fileSize)}</span>
                      <time>{formatTime(document.updatedAt)}</time>
                    </div>
                    {document.errorMessage ? (
                      <p className="document-error">{document.errorMessage}</p>
                    ) : null}
                  </article>
                ))
            )}
          </div>
        </section>

        <section className="knowledge-card">
          <div className="panel-header">
            <span>最近引用</span>
            <span>{latestSources.length}</span>
          </div>
          <div className="source-list">
            {latestSources.length === 0 ? (
              <div className="empty-tile compact">发送问题后，这里会显示本轮回答的来源。</div>
            ) : (
              latestSources.map((source) => (
                <a
                  key={`${source.fileName}-${source.url}`}
                  className="source-card"
                  href={source.url}
                  target="_blank"
                  rel="noreferrer"
                >
                  <strong>{source.fileName}</strong>
                  <span>{source.url}</span>
                </a>
              ))
            )}
          </div>
        </section>
      </aside>
    </div>
  );
// 说明当前这行代码的作用。
}
