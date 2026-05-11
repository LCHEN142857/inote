import type { RefObject } from "react";
import type {
  AuthResponse,
  ChatSession,
  LocalChatMessage,
  SourceReference,
  UserSettings
} from "../types";
import { formatTime } from "../utils";

type ChatWorkspaceProps = {
  authUser: AuthResponse;
  selectedSession: ChatSession | null;
  availableModels: string[];
  selectedModel: string;
  sessionsCount: number;
  completedDocuments: number;
  activeDocuments: number;
  loading: boolean;
  sending: boolean;
  error: string;
  messages: LocalChatMessage[];
  composer: string;
  userSettings: UserSettings;
  settingsSaving: boolean;
  messageEndRef: RefObject<HTMLDivElement>;
  onToggleSidebar: () => void;
  onModelChange: (value: string) => void;
  onReferenceModeChange: (value: boolean) => void;
  onComposerChange: (value: string) => void;
  onSend: (prompt?: string) => void;
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
        </div>
      </div>

      {props.error ? <div className="error-banner">{props.error}</div> : null}

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
          placeholder="输入你的问题"
          rows={1}
        />
        <div className="composer-tools">
          <select
            className="model-select"
            value={props.selectedModel}
            disabled={!props.availableModels.length || props.sending}
            onChange={(event) => props.onModelChange(event.target.value)}
            aria-label="切换模型"
          >
            {props.availableModels.map((model) => (
              <option key={model} value={model}>
                {model}
              </option>
            ))}
          </select>
          <label className="reference-switch reference-switch-inline">
            <input
              type="checkbox"
              checked={props.userSettings.answerFromReferencesOnly}
              disabled={props.settingsSaving}
              onChange={(event) => props.onReferenceModeChange(event.target.checked)}
            />
            <span className="switch-label">仅根据参考文档回答</span>
          </label>
        </div>
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
