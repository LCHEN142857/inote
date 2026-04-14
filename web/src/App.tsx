import { Fragment, useEffect, useMemo, useRef, useState } from "react";
import { api } from "./api";
import type {
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

function escapeHtml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function renderInline(text: string) {
  const tokens = text.split(/(`[^`]+`|\*\*[^*]+\*\*)/g).filter(Boolean);

  return tokens.map((token, index) => {
    if (token.startsWith("`") && token.endsWith("`")) {
      return <code key={`${token}-${index}`}>{token.slice(1, -1)}</code>;
    }

    if (token.startsWith("**") && token.endsWith("**")) {
      return <strong key={`${token}-${index}`}>{token.slice(2, -2)}</strong>;
    }

    return <Fragment key={`${token}-${index}`}>{token}</Fragment>;
  });
}

function renderRichText(content: string) {
  const blocks = content.split(/```/g);

  return blocks.map((block, index) => {
    if (index % 2 === 1) {
      const [firstLine, ...restLines] = block.split("\n");
      const language = firstLine.trim();
      const code = restLines.join("\n").trimEnd();

      return (
        <pre key={`code-${index}`} className="code-block">
          {language ? <span className="code-language">{language}</span> : null}
          <code>{code}</code>
        </pre>
      );
    }

    return (
      <Fragment key={`text-${index}`}>
        {block
          .split(/\n{2,}/)
          .map((section) => section.trim())
          .filter(Boolean)
          .map((section, sectionIndex) => {
            const lines = section.split("\n").map((line) => line.trimEnd());
            const bulletLines = lines.filter((line) => /^[-*]\s+/.test(line));
            const orderedLines = lines.filter((line) => /^\d+\.\s+/.test(line));

            if (bulletLines.length === lines.length) {
              return (
                <ul key={`ul-${index}-${sectionIndex}`}>
                  {bulletLines.map((line, lineIndex) => (
                    <li key={`li-${lineIndex}`}>{renderInline(line.replace(/^[-*]\s+/, ""))}</li>
                  ))}
                </ul>
              );
            }

            if (orderedLines.length === lines.length) {
              return (
                <ol key={`ol-${index}-${sectionIndex}`}>
                  {orderedLines.map((line, lineIndex) => (
                    <li key={`li-${lineIndex}`}>{renderInline(line.replace(/^\d+\.\s+/, ""))}</li>
                  ))}
                </ol>
              );
            }

            return (
              <p key={`p-${index}-${sectionIndex}`}>
                {lines.map((line, lineIndex) => (
                  <Fragment key={`line-${lineIndex}`}>
                    {lineIndex > 0 ? <br /> : null}
                    {renderInline(line)}
                  </Fragment>
                ))}
              </p>
            );
          })}
      </Fragment>
    );
  });
}

function buildLocalMessages(
  session: ChatSession | null,
  latestAnswer?: { answer: string; sources: SourceReference[] }
) {
  if (!session) return [];

  let answerMatched = false;
  return [...session.messages]
    .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
    .map<LocalChatMessage>((message) => {
      const canAttachSources =
        !answerMatched &&
        latestAnswer &&
        message.role.toLowerCase() === "assistant" &&
        message.content === latestAnswer.answer;

      if (canAttachSources) {
        answerMatched = true;
      }

      return {
        ...message,
        sources: canAttachSources ? latestAnswer.sources : undefined
      };
    });
}

function useStreamingText(messages: LocalChatMessage[]) {
  const assistantMessage = [...messages]
    .reverse()
    .find((message) => message.role.toLowerCase() === "assistant" && !message.pending);
  const fullText = assistantMessage?.content ?? "";
  const [visibleText, setVisibleText] = useState(fullText);

  useEffect(() => {
    if (!assistantMessage) {
      setVisibleText("");
      return;
    }

    if (fullText === visibleText) {
      return;
    }

    setVisibleText("");
    let frame = 0;
    const step = Math.max(6, Math.ceil(fullText.length / 42));
    const timer = window.setInterval(() => {
      frame += step;
      const nextText = fullText.slice(0, frame);
      setVisibleText(nextText);

      if (frame >= fullText.length) {
        window.clearInterval(timer);
      }
    }, 16);

    return () => window.clearInterval(timer);
  }, [assistantMessage?.id, fullText, visibleText]);

  return {
    streamingMessageId:
      assistantMessage && visibleText !== fullText ? assistantMessage.id : undefined,
    streamingText: visibleText
  };
}

function MessageContent({
  message,
  streamingText
}: {
  message: LocalChatMessage;
  streamingText?: string;
}) {
  const content = streamingText ?? message.content;
  const isAssistant = message.role.toLowerCase() === "assistant";

  if (!isAssistant) {
    return <div className="message-body plain-body">{content}</div>;
  }

  return (
    <div className="message-body rich-body">
      {renderRichText(content)}
      {streamingText ? <span className="streaming-caret" /> : null}
    </div>
  );
}

function SourcePreview({ sources }: { sources: SourceReference[] }) {
  return (
    <div className="inline-sources">
      {sources.map((source) => (
        <a
          key={`${source.fileName}-${source.url}`}
          href={source.url}
          target="_blank"
          rel="noreferrer"
        >
          {source.fileName}
        </a>
      ))}
    </div>
  );
}

export default function App() {
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
  const [latestAnswerMeta, setLatestAnswerMeta] = useState<{
    sessionId: string;
    answer: string;
    sources: SourceReference[];
  } | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    void bootstrap();
  }, []);

  async function bootstrap() {
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
        const session = await api.getSession(firstSessionId);
        setSelectedSession(session);
      }
    } catch (bootstrapError) {
      setError(
        bootstrapError instanceof Error ? bootstrapError.message : "初始化失败"
      );
    } finally {
      setLoading(false);
    }
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
      const session = await api.getSession(nextId);
      setSelectedSession(session);
    } else {
      setSelectedSession(null);
    }
  }

  async function refreshDocuments() {
    const documentList = await api.listDocuments();
    setDocuments(documentList);
  }

  async function handleSelectSession(sessionId: string) {
    setSelectedSessionId(sessionId);
    setSidebarOpen(false);
    setError("");
    try {
      const session = await api.getSession(sessionId);
      setSelectedSession(session);
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

    const pendingUserMessage: LocalChatMessage = {
      id: `local-user-${Date.now()}`,
      role: "user",
      content: question,
      createdAt: new Date().toISOString(),
      pending: true
    };
    const pendingAssistantMessage: LocalChatMessage = {
      id: `local-assistant-${Date.now()}`,
      role: "assistant",
      content: "正在检索知识库并生成回答…",
      createdAt: new Date().toISOString(),
      pending: true
    };
    const baseSession: ChatSession = selectedSession ?? {
      id: selectedSessionId || "draft",
      title: "新对话",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      messages: []
    };

    setSelectedSession({
      ...baseSession,
      messages: [...baseSession.messages, pendingUserMessage, pendingAssistantMessage]
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
          const session = await api.getSession(selectedSessionId);
          setSelectedSession(session);
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
      const updatedSession = await api.updateSession(sessionId, title);
      setRenamingId("");
      setRenameDraft("");
      await refreshSessions(updatedSession.id);
    } catch (renameError) {
      setError(renameError instanceof Error ? renameError.message : "重命名失败");
    }
  }

  async function handleDeleteSession(sessionId: string) {
    setError("");
    try {
      await api.deleteSession(sessionId);
      if (latestAnswerMeta?.sessionId === sessionId) {
        setLatestAnswerMeta(null);
      }
      await refreshSessions();
    } catch (deleteError) {
      setError(deleteError instanceof Error ? deleteError.message : "删除失败");
    }
  }

  const activeAnswerMeta =
    latestAnswerMeta && latestAnswerMeta.sessionId === selectedSession?.id
      ? { answer: latestAnswerMeta.answer, sources: latestAnswerMeta.sources }
      : undefined;

  const messages = useMemo(
    () => buildLocalMessages(selectedSession, activeAnswerMeta),
    [activeAnswerMeta, selectedSession]
  );

  const { streamingMessageId, streamingText } = useStreamingText(messages);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages, sending, streamingText]);

  const latestSources =
    [...messages].reverse().find((message) => message.sources?.length)?.sources ?? [];

  const completedDocuments = documents.filter((item) => item.status === "COMPLETED").length;

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

        <button className="primary-button" onClick={() => void handleNewSession()}>
          新建对话
        </button>

        <div className="panel-header">
          <span>会话列表</span>
          <span>{sessions.length}</span>
        </div>

        <div className="session-list">
          {sessions.length === 0 ? (
            <div className="empty-tile compact">还没有会话，先发起第一轮问答。</div>
          ) : (
            sessions.map((session) => (
              <article
                key={session.id}
                className={`session-card ${
                  session.id === selectedSessionId ? "active" : ""
                }`}
              >
                {renamingId === session.id ? (
                  <>
                    <input
                      className="ghost-input"
                      value={renameDraft}
                      onChange={(event) => setRenameDraft(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") {
                          void handleRenameSubmit(session.id);
                        }
                        if (event.key === "Escape") {
                          setRenamingId("");
                          setRenameDraft("");
                        }
                      }}
                      autoFocus
                    />
                    <div className="session-actions">
                      <button onClick={() => void handleRenameSubmit(session.id)}>保存</button>
                      <button
                        onClick={() => {
                          setRenamingId("");
                          setRenameDraft("");
                        }}
                      >
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
                      <strong>{session.title || "未命名会话"}</strong>
                      <span>{session.messageCount} 条消息</span>
                      <time>{formatTime(session.updatedAt)}</time>
                    </button>
                    <div className="session-actions">
                      <button
                        onClick={() => {
                          setRenamingId(session.id);
                          setRenameDraft(session.title);
                        }}
                      >
                        重命名
                      </button>
                      <button onClick={() => void handleDeleteSession(session.id)}>
                        删除
                      </button>
                    </div>
                  </>
                )}
              </article>
            ))
          )}
        </div>
      </aside>

      <main className="main-panel">
        <div className="topbar">
          <button
            className="icon-button mobile-only"
            onClick={() => setSidebarOpen((value) => !value)}
          >
            菜单
          </button>
          <div>
            <p className="eyebrow">RAG Workspace</p>
            <h2>{selectedSession?.title || "与你的知识库开始对话"}</h2>
          </div>
          <div className="topbar-status">
            <span>{completedDocuments} 已入库</span>
            <span>{sessions.length} 会话</span>
          </div>
        </div>

        {error ? <div className="error-banner">{escapeHtml(error)}</div> : null}

        <section className="chat-panel">
          {loading ? (
            <div className="empty-state">正在加载数据…</div>
          ) : messages.length === 0 ? (
            <div className="hero-card">
              <div className="hero-copy">
                <p className="eyebrow">Inote AI Workspace</p>
                <h3>把文档、知识与问答放进同一个工作台</h3>
                <p>
                  上传 PDF、Word、Excel、TXT 或 CSV 后，直接围绕资料提问。回答会附带来源引用，适合做总结、检索、对比和写作草稿。
                </p>
              </div>

              <div className="prompt-grid">
                {QUICK_PROMPTS.map((prompt) => (
                  <button
                    key={prompt}
                    className="prompt-card"
                    onClick={() => void handleSend(prompt)}
                  >
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
                  className={`message ${message.role.toLowerCase()} ${
                    message.pending ? "pending" : ""
                  }`}
                >
                  <div className="message-label">
                    {message.role.toLowerCase() === "user" ? "你" : "inote"}
                    <time>{formatTime(message.createdAt)}</time>
                  </div>
                  <MessageContent
                    message={message}
                    streamingText={
                      message.id === streamingMessageId ? streamingText : undefined
                    }
                  />
                  {message.sources?.length ? <SourcePreview sources={message.sources} /> : null}
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
            <button
              className="primary-button"
              onClick={() => void handleSend()}
              disabled={sending}
            >
              {sending ? "生成中…" : "发送"}
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
          <p className="panel-copy">
            支持 PDF、Word、Excel、TXT、CSV。上传后由后端解析并进入向量检索。
          </p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            hidden
            onChange={(event) => void handleUpload(event.target.files)}
          />
          <button
            className="primary-button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
          >
            {uploading ? "上传中…" : "上传文件"}
          </button>
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
                .sort(
                  (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
                )
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
              <div className="empty-tile compact">发送问题后，这里会展示本轮回答的来源。</div>
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
