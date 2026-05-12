import { useMemo, useState, type RefObject } from "react";
import type { DocumentStatus, SourceReference } from "../types";
import { formatFileSize, formatTime } from "../utils";

type KnowledgePanelProps = {
  documents: DocumentStatus[];
  uploading: boolean;
  activeDocuments: number;
  latestSources: SourceReference[];
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

export function KnowledgePanel(props: KnowledgePanelProps) {
  const [documentQuery, setDocumentQuery] = useState("");
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
          <span>文档状态</span>
          <button className="text-button" onClick={props.onRefreshDocuments}>
            刷新
          </button>
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

      <section className="knowledge-card">
        <div className="panel-header">
          <span>最近引用</span>
          <span>{props.latestSources.length}</span>
        </div>
        <div className="source-list">
          {props.latestSources.length === 0 ? (
            <div className="empty-tile compact">发送问题后，这里会显示本轮回答的来源。</div>
          ) : (
            props.latestSources.map((source) => (
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
  );
}
