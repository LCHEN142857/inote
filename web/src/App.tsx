import { useEffect, useMemo, useRef, useState } from "react";
import { api } from "./api";
import type {
  AuthCaptcha,
  AuthResponse,
  ChatSession,
  ChatSessionSummary,
  DocumentStatus,
  LocalChatMessage,
  SourceReference
} from "./types";

const QUICK_PROMPTS = [
  "总结已上传文档中的核心观点",
  "对比不同文档里的关键结论",
  "基于资料生成一份项目周报",
  "提取文档中的时间线和责任人"
];

const PINNED_SESSIONS_KEY = "inote-pinned-sessions";
const ACTIVE_DOCUMENT_STATUSES = new Set(["PENDING", "PARSING", "PROCESSING"]);

function formatTime(value?: string) {
  if (!value) return "";
  return new Intl.DateTimeFormat("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  }).format(new Date(value));
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getStoredPinnedSessions() {
  try {
    const raw = window.localStorage.getItem(PINNED_SESSIONS_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((item) => typeof item === "string") : [];
  } catch {
    return [];
  }
}

function buildMessages(
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

function AuthView(props: {
  captcha: AuthCaptcha | null;
  loading: boolean;
  submitting: boolean;
  error: string;
  onRefresh: () => void;
  onSubmit: (payload: { username: string; password: string; captchaCode: string }) => void;
}) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");

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
}

export default function App() {
  const [authUser, setAuthUser] = useState<AuthResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [authSubmitting, setAuthSubmitting] = useState(false);
  const [authError, setAuthError] = useState("");
  const [captcha, setCaptcha] = useState<AuthCaptcha | null>(null);

  const [sessions, setSessions] = useState<ChatSessionSummary[]>([]);
  const [documents, setDocuments] = useState<DocumentStatus[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [selectedSession, setSelectedSession] = useState<ChatSession | null>(null);
  const [composer, setComposer] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [error, setError] = useState("");
  const [renamingId, setRenamingId] = useState("");
  const [renameDraft, setRenameDraft] = useState("");
  const [sessionQuery, setSessionQuery] = useState("");
  const [pinnedSessionIds, setPinnedSessionIds] = useState<string[]>([]);
  const [latestAnswerMeta, setLatestAnswerMeta] = useState<{
    sessionId: string;
    answer: string;
    sources: SourceReference[];
  } | null>(null);
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setPinnedSessionIds(getStoredPinnedSessions());
    void bootstrapAuth();
  }, []);

  useEffect(() => {
    window.localStorage.setItem(PINNED_SESSIONS_KEY, JSON.stringify(pinnedSessionIds));
  }, [pinnedSessionIds]);

  const activeAnswerMeta =
    latestAnswerMeta && latestAnswerMeta.sessionId === selectedSession?.id
      ? { answer: latestAnswerMeta.answer, sources: latestAnswerMeta.sources }
      : undefined;

  const messages = useMemo(
    () => buildMessages(selectedSession, activeAnswerMeta),
    [activeAnswerMeta, selectedSession]
  );

  const latestSources =
    [...messages].reverse().find((message) => message.sources?.length)?.sources ?? [];

  const completedDocuments = documents.filter((item) => item.status === "COMPLETED").length;
  const activeDocuments = documents.filter((item) =>
    ACTIVE_DOCUMENT_STATUSES.has(item.status.toUpperCase())
  ).length;

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, sending]);

  useEffect(() => {
    if (!authUser || (!activeDocuments && !uploading)) return;

    const timer = window.setInterval(() => {
      void refreshDocuments();
    }, 3000);
    return () => window.clearInterval(timer);
  }, [activeDocuments, authUser, uploading]);

  async function refreshCaptcha() {
    const nextCaptcha = await api.getCaptcha();
    setCaptcha(nextCaptcha);
  }

  async function bootstrapAuth() {
    setAuthLoading(true);
    setAuthError("");
    try {
      await refreshCaptcha();
      const token = window.localStorage.getItem("inote-auth-token");
      if (!token) {
        api.setToken("");
        setAuthUser(null);
        return;
      }

      api.setToken(token);
      const user = await api.me();
      setAuthUser(user);
      await bootstrapWorkspace();
    } catch {
      api.setToken("");
      setAuthUser(null);
    } finally {
      setAuthLoading(false);
    }
  }

  async function bootstrapWorkspace() {
    setLoading(true);
    setError("");
    try {
      const [sessionList, documentList] = await Promise.all([
        api.listSessions(),
        api.listDocuments()
      ]);
      setSessions(sessionList);
      setDocuments(documentList);

      const firstSessionId = sessionList[0]?.id ?? "";
      setSelectedSessionId(firstSessionId);
      if (firstSessionId) {
        setSelectedSession(await api.getSession(firstSessionId));
      } else {
        setSelectedSession(null);
      }
    } catch (bootstrapError) {
      setError(bootstrapError instanceof Error ? bootstrapError.message : "初始化失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(payload: {
    username: string;
    password: string;
    captchaCode: string;
  }) {
    if (!captcha) return;
    setAuthSubmitting(true);
    setAuthError("");
    try {
      const user = await api.login({
        username: payload.username,
        password: payload.password,
        captchaId: captcha.captchaId,
        captchaCode: payload.captchaCode
      });
      api.setToken(user.token);
      setAuthUser(user);
      await bootstrapWorkspace();
      await refreshCaptcha();
    } catch (loginError) {
      setAuthError(loginError instanceof Error ? loginError.message : "登录失败");
      await refreshCaptcha();
    } finally {
      setAuthSubmitting(false);
    }
  }

  function handleLogout() {
    api.setToken("");
    setAuthUser(null);
    setSessions([]);
    setDocuments([]);
    setSelectedSessionId("");
    setSelectedSession(null);
    setLatestAnswerMeta(null);
    setComposer("");
    setError("");
    setPasswordDialogOpen(false);
    void refreshCaptcha();
  }

  async function refreshSessions(targetId?: string) {
    const sessionList = await api.listSessions();
    setSessions(sessionList);
    const nextId =
      targetId ??
      (sessionList.some((item) => item.id === selectedSessionId)
        ? selectedSessionId
        : sessionList[0]?.id ?? "");

    setSelectedSessionId(nextId);
    if (nextId) {
      setSelectedSession(await api.getSession(nextId));
    } else {
      setSelectedSession(null);
    }
  }

  async function refreshDocuments() {
    try {
      setDocuments(await api.listDocuments());
    } catch (documentError) {
      setError(documentError instanceof Error ? documentError.message : "刷新文档失败");
    }
  }

  async function handleSelectSession(sessionId: string) {
    setSelectedSessionId(sessionId);
    setSidebarOpen(false);
    setError("");
    try {
      setSelectedSession(await api.getSession(sessionId));
    } catch (sessionError) {
      setError(sessionError instanceof Error ? sessionError.message : "加载会话失败");
    }
  }

  async function handleNewSession() {
    setError("");
    try {
      const session = await api.createSession();
      setLatestAnswerMeta(null);
      setComposer("");
      setSidebarOpen(false);
      await refreshSessions(session.id);
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : "创建会话失败");
    }
  }

  async function handleSend(prompt?: string) {
    const question = (prompt ?? composer).trim();
    if (!question || sending) return;

    setSending(true);
    setError("");
    const now = new Date().toISOString();
    const baseSession: ChatSession = selectedSession ?? {
      id: selectedSessionId || "draft",
      title: "新会话",
      createdAt: now,
      updatedAt: now,
      messages: []
    };

    setSelectedSession({
      ...baseSession,
      messages: [
        ...baseSession.messages,
        { id: `u-${Date.now()}`, role: "user", content: question, createdAt: now },
        {
          id: `a-${Date.now()}`,
          role: "assistant",
          content: "正在检索你的知识文档并生成回答...",
          createdAt: now
        }
      ]
    });
    setComposer("");

    try {
      const response = await api.query(selectedSessionId || undefined, question);
      setLatestAnswerMeta({
        sessionId: response.sessionId,
        answer: response.answer,
        sources: response.sources ?? []
      });
      await refreshSessions(response.sessionId);
    } catch (queryError) {
      setError(queryError instanceof Error ? queryError.message : "发送失败");
      if (selectedSessionId) {
        try {
          setSelectedSession(await api.getSession(selectedSessionId));
        } catch {
          setSelectedSession(null);
        }
      } else {
        setSelectedSession(null);
      }
    } finally {
      setSending(false);
    }
  }

  async function handleUpload(files: FileList | null) {
    if (!files?.length || uploading) return;
    setUploading(true);
    setError("");
    try {
      await Promise.all(Array.from(files).map((file) => api.uploadDocument(file)));
      await refreshDocuments();
    } catch (uploadError) {
      setError(uploadError instanceof Error ? uploadError.message : "上传失败");
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  }

  async function handleRenameSubmit(sessionId: string) {
    const title = renameDraft.trim();
    if (!title) return;
    setError("");
    try {
      const updated = await api.updateSession(sessionId, title);
      setRenamingId("");
      setRenameDraft("");
      await refreshSessions(updated.id);
    } catch (renameError) {
      setError(renameError instanceof Error ? renameError.message : "重命名失败");
    }
  }

  async function handleDeleteSession(sessionId: string) {
    setError("");
    try {
      await api.deleteSession(sessionId);
      setPinnedSessionIds((current) => current.filter((item) => item !== sessionId));
      if (latestAnswerMeta?.sessionId === sessionId) {
        setLatestAnswerMeta(null);
      }
      await refreshSessions();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "删除失败");
    }
  }

  async function handleResetPassword() {
    setPasswordSubmitting(true);
    setError("");
    try {
      const response = await api.resetPassword(newPassword, confirmPassword);
      setNewPassword("");
      setConfirmPassword("");
      setPasswordDialogOpen(false);
      setError(response.message);
    } catch (passwordError) {
      setError(passwordError instanceof Error ? passwordError.message : "密码重置失败");
    } finally {
      setPasswordSubmitting(false);
    }
  }

  function togglePinSession(sessionId: string) {
    setPinnedSessionIds((current) =>
      current.includes(sessionId)
        ? current.filter((item) => item !== sessionId)
        : [sessionId, ...current]
    );
  }

  const filteredSessions = useMemo(() => {
    const normalized = sessionQuery.trim().toLowerCase();
    const filtered = normalized
      ? sessions.filter((session) => session.title.toLowerCase().includes(normalized))
      : sessions;

    return filtered.slice().sort((left, right) => {
      const leftPinned = pinnedSessionIds.includes(left.id) ? 1 : 0;
      const rightPinned = pinnedSessionIds.includes(right.id) ? 1 : 0;
      if (leftPinned !== rightPinned) return rightPinned - leftPinned;
      return new Date(right.updatedAt).getTime() - new Date(left.updatedAt).getTime();
    });
  }, [pinnedSessionIds, sessionQuery, sessions]);

  if (!authUser) {
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
  }

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
}
