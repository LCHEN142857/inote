import type { RefObject } from "react";
import type { DocumentStatus, SourceReference } from "../types";
import { formatFileSize, formatTime } from "../utils";

type KnowledgePanelProps = {
  documents: DocumentStatus[];
  uploading: boolean;
  activeDocuments: number;
  latestSources: SourceReference[];
  fileInputRef: RefObject<HTMLInputElement>;
  onUpload: (files: FileList | null) => void;
  onRefreshDocuments: () => void;
};

export function KnowledgePanel(props: KnowledgePanelProps) {
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
        <div className="document-list">
          {props.documents.length === 0 ? (
            <div className="empty-tile compact">还没有文档。</div>
          ) : (
            props.documents
              .slice()
              .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
              .map((document) => (
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
