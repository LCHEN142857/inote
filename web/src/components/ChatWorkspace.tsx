import type { RefObject } from "react";
import type { AuthResponse, ChatSession, LocalChatMessage, SourceReference } from "../types";
import { formatTime, QUICK_PROMPTS } from "../utils";

type ChatWorkspaceProps = {
  authUser: AuthResponse;
  selectedSession: ChatSession | null;
  sessionsCount: number;
  completedDocuments: number;
  activeDocuments: number;
  loading: boolean;
  sending: boolean;
  error: string;
  messages: LocalChatMessage[];
  composer: string;
  passwordDialogOpen: boolean;
  passwordSubmitting: boolean;
  newPassword: string;
  confirmPassword: string;
  messageEndRef: RefObject<HTMLDivElement>;
  onToggleSidebar: () => void;
  onOpenPasswordDialog: () => void;
  onClosePasswordDialog: () => void;
  onLogout: () => void;
  onComposerChange: (value: string) => void;
  onSend: (prompt?: string) => void;
  onNewPasswordChange: (value: string) => void;
  onConfirmPasswordChange: (value: string) => void;
  onResetPassword: () => void;
};

function MessageSources(props: { sources: SourceReference[] }) {
  return (
    <div className="inline-sources">
      {props.sources.map((source) => (
        <a key={`${source.fileName}-${source.url}`} href={source.url} target="_blank" rel="noreferrer">
          {source.fileName}
        </a>
      ))}
    </div>
  );
}

export function ChatWorkspace(props: ChatWorkspaceProps) {
  return (
    <main className="main-panel">
      <div className="topbar">
        <button className="icon-button mobile-only" onClick={props.onToggleSidebar}>
          菜单
        </button>
        <div>
          <p className="eyebrow">RAG Workspace</p>
          <h2>{props.selectedSession?.title || "与你的知识库开始对话"}</h2>
        </div>
        <div className="topbar-status">
          <span>{props.completedDocuments} 已入库</span>
          <span>{props.sessionsCount} 会话</span>
          {props.activeDocuments ? <span>{props.activeDocuments} 文档处理中</span> : null}
          <button className="text-button" onClick={props.onOpenPasswordDialog}>
            忘记密码
          </button>
          <button className="text-button" onClick={props.onLogout}>
            退出登录
          </button>
        </div>
      </div>

      {props.error ? <div className="error-banner">{props.error}</div> : null}

      {props.passwordDialogOpen ? (
        <section className="password-card">
          <div className="panel-header">
            <span>重置密码</span>
            <button className="text-button" onClick={props.onClosePasswordDialog}>
              关闭
            </button>
          </div>
          <div className="password-grid">
            <input
              type="password"
              value={props.newPassword}
              onChange={(event) => props.onNewPasswordChange(event.target.value)}
              placeholder="输入新密码"
            />
            <input
              type="password"
              value={props.confirmPassword}
              onChange={(event) => props.onConfirmPasswordChange(event.target.value)}
              placeholder="确认新密码"
            />
            <button
              className="primary-button"
              onClick={props.onResetPassword}
              disabled={props.passwordSubmitting}
            >
              {props.passwordSubmitting ? "提交中" : "提交"}
            </button>
          </div>
        </section>
      ) : null}

      <section className="chat-panel">
        {props.loading ? (
          <div className="empty-state">正在加载数据...</div>
        ) : props.messages.length === 0 ? (
          <div className="hero-card">
            <div className="hero-copy">
              <p className="eyebrow">Inote AI Workspace</p>
              <h3>把文档、知识与问答放进同一个工作台</h3>
              <p>已登录用户只能查看自己的文档和会话。上传文件后，直接围绕资料提问，回答会附带来源引用。</p>
            </div>
            <div className="prompt-grid">
              {QUICK_PROMPTS.map((prompt) => (
                <button key={prompt} className="prompt-card" onClick={() => props.onSend(prompt)}>
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="message-stream">
            {props.messages.map((message) => (
              <article
                key={message.id}
                className={`message ${message.role.toLowerCase()} ${message.pending ? "pending" : ""}`}
              >
                <div className="message-label">
                  {message.role.toLowerCase() === "user" ? props.authUser.username : "inote"}
                  <time>{formatTime(message.createdAt)}</time>
                </div>
                <div className="message-body plain-body">{message.content}</div>
                {message.sources?.length ? <MessageSources sources={message.sources} /> : null}
              </article>
            ))}
            <div ref={props.messageEndRef} />
          </div>
        )}
      </section>

      <div className="composer-panel">
        <textarea
          value={props.composer}
          onChange={(event) => props.onComposerChange(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              props.onSend();
            }
          }}
          placeholder="输入你的问题，例如：这批文档里有哪些关键风险与行动项？"
          rows={1}
        />
        <div className="composer-actions">
          <span>Enter 发送，Shift + Enter 换行</span>
          <button className="primary-button" onClick={() => props.onSend()} disabled={props.sending}>
            {props.sending ? "生成中" : "发送"}
          </button>
        </div>
      </div>
    </main>
  );
}
