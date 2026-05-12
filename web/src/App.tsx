import { useEffect, useMemo, useRef, useState } from "react";
import { ApiError, api } from "./api";
import { AuthView } from "./components/AuthView";
import { ChatWorkspace } from "./components/ChatWorkspace";
import { KnowledgePanel } from "./components/KnowledgePanel";
import { SessionSidebar } from "./components/SessionSidebar";
import type {
  AuthCaptcha,
  AuthResponse,
  ChatSession,
  ChatSessionSummary,
  DocumentStatus,
  SourceReference,
  UserSettings
} from "./types";
import {
  ACTIVE_DOCUMENT_STATUSES,
  PINNED_SESSIONS_KEY,
  buildMessages,
  getStoredPinnedSessions
} from "./utils";

const CHAT_MODEL_STORAGE_KEY = "inote-chat-model";

export default function App() {
  const [authUser, setAuthUser] = useState<AuthResponse | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [authSubmitting, setAuthSubmitting] = useState(false);
  const [authError, setAuthError] = useState("");
  const [captcha, setCaptcha] = useState<AuthCaptcha | null>(null);
  const [loginLockUntil, setLoginLockUntil] = useState(0);
  const [loginLockRemaining, setLoginLockRemaining] = useState(0);

  const [sessions, setSessions] = useState<ChatSessionSummary[]>([]);
  const [documents, setDocuments] = useState<DocumentStatus[]>([]);
  const [selectedSessionId, setSelectedSessionId] = useState("");
  const [selectedSession, setSelectedSession] = useState<ChatSession | null>(null);
  const [composer, setComposer] = useState("");
  const [availableModels, setAvailableModels] = useState<string[]>([]);
  const [selectedModel, setSelectedModel] = useState(
    () => window.localStorage.getItem(CHAT_MODEL_STORAGE_KEY) ?? ""
  );
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
  const [userSettings, setUserSettings] = useState<UserSettings>({ answerFromReferencesOnly: true });
  const [settingsSaving, setSettingsSaving] = useState(false);
  const [currentPath, setCurrentPath] = useState(() => window.location.pathname);
  const [toastMessage, setToastMessage] = useState("");
  const [documentDeleteTarget, setDocumentDeleteTarget] = useState<DocumentStatus | null>(null);
  const [documentDeleting, setDocumentDeleting] = useState(false);

  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);
  const authExpiryHandledRef = useRef(false);

  function navigateTo(path: "/login" | "/index", replace = true) {
    if (window.location.pathname !== path) {
      if (replace) {
        window.history.replaceState({}, "", path);
      } else {
        window.history.pushState({}, "", path);
      }
    }
    setCurrentPath(path);
  }

  function clearWorkspaceState() {
    setAuthUser(null);
    setSessions([]);
    setDocuments([]);
    setSelectedSessionId("");
    setSelectedSession(null);
    setLatestAnswerMeta(null);
    setComposer("");
    setError("");
    setPasswordDialogOpen(false);
    setUserSettings({ answerFromReferencesOnly: true });
    setAvailableModels([]);
    setLoginLockUntil(0);
    setLoginLockRemaining(0);
  }

  function showToast(message: string) {
    setToastMessage(message);
  }

  function handleAuthExpired() {
    if (authExpiryHandledRef.current) return;
    authExpiryHandledRef.current = true;
    api.setToken("");
    clearWorkspaceState();
    showToast("登录信息已过期，请重新登录");
    navigateTo("/login");
    void refreshCaptcha();
  }

  function isUnauthorizedError(error: unknown) {
    return error instanceof ApiError && error.status === 401;
  }

  useEffect(() => {
    setPinnedSessionIds(getStoredPinnedSessions());
    void bootstrapAuth();
  }, []);

  useEffect(() => {
    window.localStorage.setItem(PINNED_SESSIONS_KEY, JSON.stringify(pinnedSessionIds));
  }, [pinnedSessionIds]);

  useEffect(() => {
    if (loginLockUntil <= Date.now()) {
      setLoginLockRemaining(0);
      return;
    }

    const tick = () => {
      const remaining = Math.max(0, Math.ceil((loginLockUntil - Date.now()) / 1000));
      setLoginLockRemaining(remaining);
      if (remaining <= 0) {
        setLoginLockUntil(0);
        setAuthError("");
      }
    };

    tick();
    const timer = window.setInterval(tick, 250);
    return () => window.clearInterval(timer);
  }, [loginLockUntil]);

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname);
    };

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  useEffect(() => {
    if (!toastMessage) return;

    const timer = window.setTimeout(() => setToastMessage(""), 3200);
    return () => window.clearTimeout(timer);
  }, [toastMessage]);

  useEffect(() => {
    api.setUnauthorizedHandler(handleAuthExpired);
    return () => api.setUnauthorizedHandler(null);
  }, []);

  const activeAnswerMeta =
    latestAnswerMeta && latestAnswerMeta.sessionId === selectedSession?.id
      ? { answer: latestAnswerMeta.answer, sources: latestAnswerMeta.sources }
      : undefined;

  const messages = useMemo(
    () => buildMessages(selectedSession, activeAnswerMeta),
    [activeAnswerMeta, selectedSession]
  );

  const latestSources = [...messages].reverse().find((message) => message.sources?.length)?.sources ?? [];
  const completedDocuments = documents.filter((item) => item.status === "COMPLETED").length;
  const activeDocuments = documents.filter((item) =>
    ACTIVE_DOCUMENT_STATUSES.has(item.status.toUpperCase())
  ).length;

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

  useEffect(() => {
    if (authLoading) return;

    if (authUser && currentPath !== "/index") {
      navigateTo("/index");
      return;
    }

    if (!authUser && currentPath !== "/login" && !window.localStorage.getItem("inote-auth-token")) {
      navigateTo("/login");
    }
  }, [authLoading, authUser, currentPath]);

  async function refreshCaptcha() {
    setCaptcha(await api.getCaptcha());
  }

  async function bootstrapAuth() {
    setAuthLoading(true);
    setAuthError("");
    try {
      await refreshCaptcha();
      const token = window.localStorage.getItem("inote-auth-token");
      if (!token) {
        api.setToken("");
        clearWorkspaceState();
        if (currentPath !== "/login") {
          navigateTo("/login");
        }
        return;
      }

      api.setToken(token);
      const user = await api.me();
      setAuthUser(user);
      setLoginLockUntil(0);
      setLoginLockRemaining(0);
      navigateTo("/index");
      await bootstrapWorkspace();
    } catch {
      api.setToken("");
      clearWorkspaceState();
      if (currentPath !== "/login") {
        navigateTo("/login");
      }
    } finally {
      setAuthLoading(false);
    }
  }

  async function bootstrapWorkspace() {
    setLoading(true);
    setError("");
    try {
      const [sessionList, documentList, settings, modelCatalog] = await Promise.all([
        api.listSessions(),
        api.listDocuments(),
        api.getSettings(),
        api.getChatModels()
      ]);

      setSessions(sessionList);
      setDocuments(documentList);
      setUserSettings(settings);
      setAvailableModels(modelCatalog.availableModels);

      const storedModel = window.localStorage.getItem(CHAT_MODEL_STORAGE_KEY) ?? "";
      const nextModel =
        storedModel && modelCatalog.availableModels.includes(storedModel)
          ? storedModel
          : modelCatalog.defaultModel;
      setSelectedModel(nextModel);
      window.localStorage.setItem(CHAT_MODEL_STORAGE_KEY, nextModel);

      const firstSessionId = sessionList[0]?.id ?? "";
      setSelectedSessionId(firstSessionId);
      if (firstSessionId) {
        setSelectedSession(await api.getSession(firstSessionId));
      } else {
        setSelectedSession(null);
      }
    } catch (bootstrapError) {
      if (isUnauthorizedError(bootstrapError)) return;
      setError(bootstrapError instanceof Error ? bootstrapError.message : "初始化失败");
    } finally {
      setLoading(false);
    }
  }

  async function handleLogin(payload: { username: string; password: string; captchaCode: string }) {
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
      authExpiryHandledRef.current = false;
      setAuthUser(user);
      setLoginLockUntil(0);
      setLoginLockRemaining(0);
      navigateTo("/index");
      await bootstrapWorkspace();
      await refreshCaptcha();
    } catch (loginError) {
      if (loginError instanceof ApiError && loginError.status === 423) {
        const lockedUntil = Number(loginError.details.lockedUntilEpochMillis);
        const remaining = Number(loginError.details.lockSeconds) || 60;
        setLoginLockUntil(
          Number.isFinite(lockedUntil) && lockedUntil > Date.now()
            ? lockedUntil
            : Date.now() + remaining * 1000
        );
        setAuthError(loginError.message);
      } else {
        setAuthError(loginError instanceof Error ? loginError.message : "登录失败");
      }
      await refreshCaptcha();
    } finally {
      setAuthSubmitting(false);
    }
  }

  function handleLogout() {
    authExpiryHandledRef.current = false;
    api.setToken("");
    clearWorkspaceState();
    navigateTo("/login");
    void refreshCaptcha();
  }

  async function refreshSessions(targetId?: string) {
    const sessionList = await api.listSessions();
    setSessions(sessionList);
    const nextId =
      targetId ??
      (sessionList.some((item) => item.id === selectedSessionId) ? selectedSessionId : sessionList[0]?.id ?? "");

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
      if (isUnauthorizedError(documentError)) return;
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
      if (isUnauthorizedError(sessionError)) return;
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
      if (isUnauthorizedError(createError)) return;
      setError(createError instanceof Error ? createError.message : "创建会话失败");
    }
  }

  async function handleSend(prompt?: string) {
    const question = (prompt ?? composer).trim();
    if (!question || sending) return;

    setSending(true);
    setError("");
    let optimisticSession: ChatSession | null = null;
    let optimisticAssistantMessageId = "";

    try {
      const now = new Date().toISOString();
      const ensuredSessionId = selectedSessionId || (await api.createSession()).id;
      const baseSession: ChatSession = selectedSession ?? {
        id: ensuredSessionId,
        title: "新会话",
        createdAt: now,
        updatedAt: now,
        messages: []
      };

      const messageSuffix = Date.now();
      optimisticAssistantMessageId = `a-${messageSuffix}`;
      optimisticSession = {
        ...baseSession,
        messages: [
          ...baseSession.messages,
          { id: `u-${messageSuffix}`, role: "user", content: question, createdAt: now },
          {
            id: optimisticAssistantMessageId,
            role: "assistant",
            content: "generating...",
            createdAt: now
          }
        ]
      };

      setSelectedSessionId(ensuredSessionId);
      setSelectedSession(optimisticSession);
      setComposer("");

      const response = await api.query(ensuredSessionId, question, selectedModel);
      const responseSessionId = response.sessionId ?? ensuredSessionId;
      setSelectedSessionId(responseSessionId);
      setLatestAnswerMeta({
        sessionId: responseSessionId,
        answer: response.answer,
        sources: response.sources ?? []
      });
      await refreshSessions(responseSessionId);
    } catch (queryError) {
      if (isUnauthorizedError(queryError)) return;
      if (queryError instanceof ApiError && queryError.status >= 500 && optimisticSession) {
        setError("");
        setSelectedSession({
          ...optimisticSession,
          messages: optimisticSession.messages.map((message) =>
            message.id === optimisticAssistantMessageId
              ? { ...message, content: "服务暂时不可用，请稍后重试" }
              : message
          )
        });
        return;
      }

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
      if (isUnauthorizedError(uploadError)) return;
      setError(uploadError instanceof Error ? uploadError.message : "上传失败");
    } finally {
      setUploading(false);
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  }

  async function handleDeleteFailedDocument() {
    if (!documentDeleteTarget || documentDeleting) return;

    setDocumentDeleting(true);
    setError("");
    try {
      await api.deleteDocument(documentDeleteTarget.documentId);
      setDocumentDeleteTarget(null);
      await refreshDocuments();
    } catch (documentDeleteError) {
      if (isUnauthorizedError(documentDeleteError)) return;
      setError(documentDeleteError instanceof Error ? documentDeleteError.message : "删除文件失败");
    } finally {
      setDocumentDeleting(false);
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
      if (isUnauthorizedError(renameError)) return;
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
      if (isUnauthorizedError(deleteError)) return;
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
      if (isUnauthorizedError(passwordError)) return;
      setError(passwordError instanceof Error ? passwordError.message : "密码重置失败");
    } finally {
      setPasswordSubmitting(false);
    }
  }

  async function handleReferenceModeChange(answerFromReferencesOnly: boolean) {
    if (settingsSaving) return;

    const previous = userSettings;
    setSettingsSaving(true);
    setError("");
    setUserSettings({ answerFromReferencesOnly });
    try {
      setUserSettings(await api.updateSettings(answerFromReferencesOnly));
    } catch (settingsError) {
      if (isUnauthorizedError(settingsError)) return;
      setUserSettings(previous);
      setError(settingsError instanceof Error ? settingsError.message : "保存回答设置失败");
    } finally {
      setSettingsSaving(false);
    }
  }

  function handleModelChange(model: string) {
    setSelectedModel(model);
    window.localStorage.setItem(CHAT_MODEL_STORAGE_KEY, model);
  }

  function togglePinSession(sessionId: string) {
    setPinnedSessionIds((current) =>
      current.includes(sessionId) ? current.filter((item) => item !== sessionId) : [sessionId, ...current]
    );
  }

  if (!authUser) {
    return (
      <>
        {toastMessage ? <div className="toast-banner">{toastMessage}</div> : null}
        <AuthView
          captcha={captcha}
          loading={authLoading}
          submitting={authSubmitting}
          error={authError}
          loginLockRemaining={loginLockRemaining}
          onRefresh={() => void refreshCaptcha()}
          onSubmit={(payload) => void handleLogin(payload)}
        />
      </>
    );
  }

  return (
    <>
      {toastMessage ? <div className="toast-banner">{toastMessage}</div> : null}
      <div className="app-shell">
        <SessionSidebar
          authUsername={authUser.username}
          sidebarOpen={sidebarOpen}
          sessionQuery={sessionQuery}
          filteredSessions={filteredSessions}
          pinnedSessionIds={pinnedSessionIds}
          selectedSessionId={selectedSessionId}
          renamingId={renamingId}
          renameDraft={renameDraft}
          passwordDialogOpen={passwordDialogOpen}
          passwordSubmitting={passwordSubmitting}
          newPassword={newPassword}
          confirmPassword={confirmPassword}
          onSessionQueryChange={setSessionQuery}
          onRenameDraftChange={setRenameDraft}
          onSelectSession={(sessionId) => void handleSelectSession(sessionId)}
          onNewSession={() => void handleNewSession()}
          onTogglePinSession={togglePinSession}
          onStartRename={(sessionId, title) => {
            setRenamingId(sessionId);
            setRenameDraft(title);
          }}
          onCancelRename={() => {
            setRenamingId("");
            setRenameDraft("");
          }}
          onSubmitRename={(sessionId) => void handleRenameSubmit(sessionId)}
          onDeleteSession={(sessionId) => void handleDeleteSession(sessionId)}
          onOpenPasswordDialog={() => setPasswordDialogOpen(true)}
          onClosePasswordDialog={() => setPasswordDialogOpen(false)}
          onLogout={handleLogout}
          onNewPasswordChange={setNewPassword}
          onConfirmPasswordChange={setConfirmPassword}
          onResetPassword={() => void handleResetPassword()}
        />

        <ChatWorkspace
          authUser={authUser}
          selectedSession={selectedSession}
          availableModels={availableModels}
          selectedModel={selectedModel}
          sessionsCount={sessions.length}
          completedDocuments={completedDocuments}
          activeDocuments={activeDocuments}
          loading={loading}
          sending={sending}
          error={error}
          messages={messages}
          composer={composer}
          userSettings={userSettings}
          settingsSaving={settingsSaving}
          messageEndRef={messageEndRef}
          onToggleSidebar={() => setSidebarOpen((value) => !value)}
          onModelChange={handleModelChange}
          onReferenceModeChange={(value) => void handleReferenceModeChange(value)}
          onComposerChange={setComposer}
          onSend={(prompt) => void handleSend(prompt)}
        />

        <KnowledgePanel
          documents={documents}
          uploading={uploading}
          activeDocuments={activeDocuments}
          latestSources={latestSources}
          fileInputRef={fileInputRef}
          onUpload={(files) => void handleUpload(files)}
          onDeleteDocument={setDocumentDeleteTarget}
          onRefreshDocuments={() => void refreshDocuments()}
        />
      </div>
      {documentDeleteTarget ? (
        <div className="confirm-overlay" role="presentation">
          <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="delete-document-title">
            <h3 id="delete-document-title">确认删除失败文件？</h3>
            <p>{documentDeleteTarget.fileName}</p>
            <div className="confirm-actions">
              <button
                className="text-button"
                type="button"
                disabled={documentDeleting}
                onClick={() => setDocumentDeleteTarget(null)}
              >
                取消
              </button>
              <button
                className="danger-button"
                type="button"
                disabled={documentDeleting}
                onClick={() => void handleDeleteFailedDocument()}
              >
                {documentDeleting ? "删除中" : "确认删除"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
