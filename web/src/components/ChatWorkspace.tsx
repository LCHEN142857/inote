import type { RefObject } from "react";
import { useEffect, useState } from "react";
import type {
  AuthResponse,
  ChatSession,
  LocalChatMessage,
  SourceReference,
  UserSettings
} from "../types";
import { formatTime } from "../utils";
import { ProjectCornerLink } from "./ProjectCornerLink";

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
  const [activeSourceKey, setActiveSourceKey] = useState("");

  return (
    <div className="inline-sources">
      {props.sources.map((source) => {
        const sourceKey = `${source.fileName}-${source.url}-${source.preview ?? ""}`;
        const isActive = activeSourceKey === sourceKey;

        return (
          <div className="inline-source-wrap" key={sourceKey}>
            <button
              type="button"
              className="inline-source-card"
              onClick={() => setActiveSourceKey(isActive ? "" : sourceKey)}
            >
              <strong>{source.fileName}</strong>
            </button>
            {isActive ? (
              <div className="source-popover" role="tooltip">
                <div className="source-popover-title">{source.fileName}</div>
                <p>{source.preview || "当前引用没有可预览的段落内容。"}</p>
              </div>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

function PendingAnswer() {
  const [dotCount, setDotCount] = useState(0);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setDotCount((value) => (value + 1) % 4);
    }, 450);

    return () => window.clearInterval(timer);
  }, []);

  return <span className="generating-text">Generating{".".repeat(dotCount)}</span>;
}

function MarkdownBody(props: { content: string }) {
  return (
    <div className="message-body rich-body">
      {parseMarkdownBlocks(props.content).map((block, index) => {
        if (block.type === "ul") {
          return (
            <ul key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInlineMarkdown(item)}</li>
              ))}
            </ul>
          );
        }

        if (block.type === "ol") {
          return (
            <ol key={index}>
              {block.items.map((item, itemIndex) => (
                <li key={itemIndex}>{renderInlineMarkdown(item)}</li>
              ))}
            </ol>
          );
        }

        return <p key={index}>{renderInlineMarkdown(block.text)}</p>;
      })}
    </div>
  );
}

type MarkdownBlock =
  | { type: "p"; text: string }
  | { type: "ul"; items: string[] }
  | { type: "ol"; items: string[] };

function parseMarkdownBlocks(content: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = [];
  let paragraph: string[] = [];
  let list: { type: "ul" | "ol"; items: string[] } | null = null;

  function flushParagraph() {
    if (!paragraph.length) return;
    blocks.push({ type: "p", text: paragraph.join("\n") });
    paragraph = [];
  }

  function flushList() {
    if (!list) return;
    blocks.push(list);
    list = null;
  }

  content.split(/\r?\n/).forEach((line) => {
    const trimmed = line.trim();
    const unordered = trimmed.match(/^[-*]\s+(.+)$/);
    const ordered = trimmed.match(/^\d+[.)]\s+(.+)$/);

    if (!trimmed) {
      flushParagraph();
      flushList();
      return;
    }

    if (unordered) {
      flushParagraph();
      if (!list || list.type !== "ul") {
        flushList();
        list = { type: "ul", items: [] };
      }
      list.items.push(unordered[1]);
      return;
    }

    if (ordered) {
      flushParagraph();
      if (!list || list.type !== "ol") {
        flushList();
        list = { type: "ol", items: [] };
      }
      list.items.push(ordered[1]);
      return;
    }

    flushList();
    paragraph.push(trimmed);
  });

  flushParagraph();
  flushList();
  return blocks.length ? blocks : [{ type: "p", text: content }];
}

function renderInlineMarkdown(text: string) {
  const parts = text.split(/(\*\*[^*]+\*\*|`[^`]+`)/g).filter(Boolean);
  return parts.map((part, index) => {
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={index}>{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={index}>{part.slice(1, -1)}</code>;
    }
    return part;
  });
}

function MessageBody(props: { message: LocalChatMessage }) {
  if (props.message.pending && !props.message.content) {
    return (
      <div className="message-body plain-body pending-body">
        <PendingAnswer />
      </div>
    );
  }

  if (props.message.role.toLowerCase() === "assistant" && !props.message.pending) {
    return <MarkdownBody content={props.message.content} />;
  }

  return <div className="message-body plain-body">{props.message.content}</div>;
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
                <MessageBody message={message} />
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
      <ProjectCornerLink />
    </main>
  );
}
