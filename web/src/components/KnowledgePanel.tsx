import { useEffect, useMemo, useRef, useState, type CSSProperties, type RefObject } from "react";
import { createPortal } from "react-dom";
import type { ChatSession, DocumentStatus, SourceReference } from "../types";
import { formatFileSize, formatTime } from "../utils";

type KnowledgePanelProps = {
  documents: DocumentStatus[];
  uploading: boolean;
  activeDocuments: number;
  latestSources: SourceReference[];
  sessions: ChatSession[];
  fileInputRef: RefObject<HTMLInputElement>;
  onUpload: (files: FileList | null) => void;
  onDeleteDocument: (document: DocumentStatus) => void;
  onRefreshDocuments: () => void;
};

function TrashIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M9 3h6l1 2h4v2H4V5h4l1-2Zm-2 6h10l-.7 11H7.7L7 9Zm3 2v7h2v-7h-2Zm4 0v7h2v-7h-2Z" />
    </svg>
  );
}

function InfoIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M11 10h2v7h-2v-7Zm0-3h2v2h-2V7Zm1-5a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm0 18a8 8 0 1 1 0-16 8 8 0 0 1 0 16Z" />
    </svg>
  );
}

const documentStatusTitleStyle: CSSProperties = {
  color: "var(--text-main)",
  fontSize: 15,
  fontWeight: 650
};

const documentStatusHeadingStyle: CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  gap: 8
};

const documentStatusActionsStyle: CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  gap: 8
};

const documentInfoWrapStyle: CSSProperties = {
  position: "relative",
  display: "inline-flex"
};

const documentInfoButtonStyle: CSSProperties = {
  display: "grid",
  placeItems: "center",
  width: 32,
  height: 32,
  borderRadius: 999,
  background: "rgba(247, 247, 248, 0.95)",
  color: "var(--text-soft)"
};

const documentInfoIconStyle: CSSProperties = {
  width: 18,
  height: 18,
  fill: "currentColor"
};

const documentInfoTipStyle: CSSProperties = {
  position: "absolute",
  top: "calc(100% + 10px)",
  right: 0,
  zIndex: 8,
  width: 260,
  borderRadius: 16,
  border: "1px solid var(--line-soft)",
  background: "rgba(255, 255, 255, 0.98)",
  color: "var(--text-soft)",
  padding: "10px 12px",
  boxShadow: "0 14px 32px rgba(15, 23, 42, 0.14)",
  fontSize: 13,
  lineHeight: 1.6,
  textAlign: "left",
  pointerEvents: "none"
};

type ReferencedSessionSummary = {
  sessionId: string;
  title: string;
  count: number;
};

type ReferencedDocumentSummary = {
  fileName: string;
  count: number;
  sessions: ReferencedSessionSummary[];
};

function buildReferencedDocuments(sessions: ChatSession[], latestSources: SourceReference[]) {
  const documentStats = new Map<string, { count: number; sessions: Map<string, ReferencedSessionSummary> }>();

  const addSource = (source: SourceReference, session: ChatSession) => {
    const fileName = source.fileName.trim();
    if (!fileName) return;

    const documentStat = documentStats.get(fileName) ?? { count: 0, sessions: new Map() };
    const sessionStat = documentStat.sessions.get(session.id) ?? {
      sessionId: session.id,
      title: session.title || "未命名会话",
      count: 0
    };

    documentStat.count += 1;
    sessionStat.count += 1;
    documentStat.sessions.set(session.id, sessionStat);
    documentStats.set(fileName, documentStat);
  };

  sessions.forEach((session) => {
    session.messages.forEach((message) => {
      message.sources?.forEach((source) => addSource(source, session));
    });
  });

  if (documentStats.size === 0 && latestSources.length > 0) {
    const latestSession: ChatSession = {
      id: "latest-answer",
      title: "当前会话",
      createdAt: "",
      updatedAt: "",
      messages: []
    };
    latestSources.forEach((source) => addSource(source, latestSession));
  }

  return Array.from(documentStats.entries())
    .map<ReferencedDocumentSummary>(([fileName, stat]) => ({
      fileName,
      count: stat.count,
      sessions: Array.from(stat.sessions.values()).sort((a, b) => b.count - a.count)
    }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 3);
}

export function KnowledgePanel(props: KnowledgePanelProps) {
  const [documentQuery, setDocumentQuery] = useState("");
  const [documentInfoOpen, setDocumentInfoOpen] = useState(false);
  const [openReferencedDocument, setOpenReferencedDocument] = useState<{
    fileName: string;
    rect: DOMRect;
    sessions: ReferencedSessionSummary[];
  } | null>(null);
  const documentInfoRef = useRef<HTMLSpanElement | null>(null);
  const sourceListRef = useRef<HTMLDivElement | null>(null);
  const referenceSessionPopoverRef = useRef<HTMLDivElement | null>(null);
  const referenceSessionPopoverWidth = Math.min(280, Math.max(220, window.innerWidth - 32));
  const referenceSessionPopoverLeft = openReferencedDocument
    ? Math.min(
        Math.max(16, openReferencedDocument.rect.right - referenceSessionPopoverWidth),
        window.innerWidth - referenceSessionPopoverWidth - 16
      )
    : 16;
  const referenceSessionPopoverTop = openReferencedDocument
    ? openReferencedDocument.rect.bottom + 10
    : 0;

  const visibleDocuments = useMemo(() => {
    const keyword = documentQuery.trim().toLowerCase();
    const sorted = props.documents
      .slice()
      .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

    if (!keyword) return sorted;

    return sorted.filter(
      (document) =>
        document.fileName.toLowerCase().includes(keyword) ||
        document.status.toLowerCase().includes(keyword)
    );
  }, [documentQuery, props.documents]);

  const referencedDocuments = useMemo(
    () => buildReferencedDocuments(props.sessions, props.latestSources),
    [props.latestSources, props.sessions]
  );

  useEffect(() => {
    if (!openReferencedDocument) return;
    if (referencedDocuments.some((document) => document.fileName === openReferencedDocument.fileName)) return;
    setOpenReferencedDocument(null);
  }, [openReferencedDocument, referencedDocuments]);

  useEffect(() => {
    if (!openReferencedDocument) return;

    const closeOnOutsideInteraction = (event: PointerEvent | FocusEvent) => {
      const target = event.target as Node;
      if (!sourceListRef.current?.contains(target) && !referenceSessionPopoverRef.current?.contains(target)) {
        setOpenReferencedDocument(null);
      }
    };
    const closeOnViewportChange = () => setOpenReferencedDocument(null);

    document.addEventListener("pointerdown", closeOnOutsideInteraction);
    document.addEventListener("focusin", closeOnOutsideInteraction);
    window.addEventListener("resize", closeOnViewportChange);
    window.addEventListener("scroll", closeOnViewportChange, true);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideInteraction);
      document.removeEventListener("focusin", closeOnOutsideInteraction);
      window.removeEventListener("resize", closeOnViewportChange);
      window.removeEventListener("scroll", closeOnViewportChange, true);
    };
  }, [openReferencedDocument]);

  useEffect(() => {
    if (!documentInfoOpen) return;

    const closeOnOutsideInteraction = (event: PointerEvent | FocusEvent) => {
      if (!documentInfoRef.current?.contains(event.target as Node)) {
        setDocumentInfoOpen(false);
      }
    };

    document.addEventListener("pointerdown", closeOnOutsideInteraction);
    document.addEventListener("focusin", closeOnOutsideInteraction);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideInteraction);
      document.removeEventListener("focusin", closeOnOutsideInteraction);
    };
  }, [documentInfoOpen]);

  return (
    <aside className="knowledge-panel">
      <section className="knowledge-card upload-card">
        <div className="panel-header">
          <span>知识库</span>
          <span>{props.documents.length}</span>
        </div>
        <p className="panel-copy">支持 PDF、Word、Excel、TXT、CSV。</p>
        <input
          ref={props.fileInputRef}
          type="file"
          multiple
          hidden
          onChange={(event) => props.onUpload(event.target.files)}
        />
        <button
          className="primary-button"
          onClick={() => props.fileInputRef.current?.click()}
          disabled={props.uploading}
        >
          {props.uploading ? "上传中" : "上传文件"}
        </button>
        {props.activeDocuments ? (
          <p className="panel-tip">检测到文档仍在处理，列表会自动轮询刷新。</p>
        ) : null}
      </section>

      <section className="knowledge-card">
        <div className="panel-header">
          <span style={documentStatusHeadingStyle}>
            <span style={documentStatusTitleStyle}>文档状态</span>
            <span style={documentInfoWrapStyle} ref={documentInfoRef}>
              <button
                style={documentInfoButtonStyle}
                type="button"
                aria-label="文档上传说明"
                title="文档上传说明"
                onClick={() => setDocumentInfoOpen((value) => !value)}
              >
                <span style={documentInfoIconStyle}>
                  <InfoIcon />
                </span>
              </button>
              {documentInfoOpen ? (
                <span style={documentInfoTipStyle}>上传相同名称的文档，以最新上传的文档信息为准</span>
              ) : null}
            </span>
          </span>
          <div style={documentStatusActionsStyle}>
            <button className="text-button" onClick={props.onRefreshDocuments}>
              刷新
            </button>
          </div>
        </div>
        <label className="document-search">
          <span className="sr-only">搜索文档</span>
          <input
            value={documentQuery}
            onChange={(event) => setDocumentQuery(event.target.value)}
            placeholder="搜索文档"
          />
        </label>
        <div className="document-list">
          {props.documents.length === 0 ? (
            <div className="empty-tile compact">还没有文档。</div>
          ) : visibleDocuments.length === 0 ? (
            <div className="empty-tile compact">没有匹配的文档</div>
          ) : (
            visibleDocuments.map((document) => {
              const failed = document.status.toUpperCase() === "FAILED";
              return (
                <article key={document.documentId} className="document-card">
                  <div className="document-title-row">
                    <strong>{document.fileName}</strong>
                    <span className={`status-pill status-${document.status.toLowerCase()}`}>
                      {document.status}
                    </span>
                    <span className={`status-pill status-${document.active === false ? "failed" : "completed"}`}>
                      {document.active === false ? "INACTIVE" : "ACTIVE"}
                    </span>
                  </div>
                  <div className="document-meta">
                    <span>{formatFileSize(document.fileSize)}</span>
                    <time>{formatTime(document.updatedAt)}</time>
                  </div>
                  {failed ? (
                    <div className="document-card-actions">
                      <button
                        className="document-delete-button"
                        type="button"
                        aria-label="删除失败文件"
                        title="删除失败文件"
                        onClick={() => props.onDeleteDocument(document)}
                      >
                        <TrashIcon />
                      </button>
                    </div>
                  ) : null}
                </article>
              );
            })
          )}
        </div>
      </section>

      <section className="knowledge-card recent-references-card">
        <div className="panel-header recent-references-header">
          <span style={documentStatusHeadingStyle}>
            <span style={documentStatusTitleStyle}>最近引用</span>
          </span>
        </div>
        <div className="source-list" ref={sourceListRef}>
          {referencedDocuments.length === 0 ? (
            <div className="empty-tile compact">发送问题后，这里会显示被引用次数最多的文档。</div>
          ) : (
            referencedDocuments.map((source) => (
              <article key={source.fileName} className="source-card referenced-document-card">
                <div className="referenced-document-title-row">
                  <strong>{source.fileName}</strong>
                  <div className="reference-count-wrap">
                    <button
                      className="reference-count-button"
                      type="button"
                      onClick={(event) => {
                        const rect = event.currentTarget.getBoundingClientRect();
                        setOpenReferencedDocument((document) =>
                          document?.fileName === source.fileName
                            ? null
                            : {
                                fileName: source.fileName,
                                rect,
                                sessions: source.sessions
                              }
                        );
                      }}
                    >
                      被引用 {source.count} 次
                    </button>
                  </div>
                </div>
              </article>
            ))
          )}
        </div>
        {openReferencedDocument
          ? createPortal(
              <div
                ref={referenceSessionPopoverRef}
                className="reference-session-popover"
                role="dialog"
                aria-label="引用此文档的会话列表"
                style={{
                  left: referenceSessionPopoverLeft,
                  top: referenceSessionPopoverTop,
                  width: referenceSessionPopoverWidth
                }}
              >
                {openReferencedDocument.sessions.map((session) => (
                  <div key={session.sessionId} className="reference-session-row">
                    <span>{session.title}</span>
                    <strong>{session.count} 次</strong>
                  </div>
                ))}
              </div>,
              document.body
            )
          : null}
      </section>
    </aside>
  );
}