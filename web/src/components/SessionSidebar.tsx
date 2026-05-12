import { useState } from "react";
import type { ChatSessionSummary } from "../types";
import { formatTime } from "../utils";

type SessionSidebarProps = {
  authUsername: string;
  sidebarOpen: boolean;
  sessionQuery: string;
  filteredSessions: ChatSessionSummary[];
  pinnedSessionIds: string[];
  selectedSessionId: string;
  renamingId: string;
  renameDraft: string;
  passwordDialogOpen: boolean;
  passwordSubmitting: boolean;
  newPassword: string;
  confirmPassword: string;
  onSessionQueryChange: (value: string) => void;
  onRenameDraftChange: (value: string) => void;
  onSelectSession: (sessionId: string) => void;
  onNewSession: () => void;
  onTogglePinSession: (sessionId: string) => void;
  onStartRename: (sessionId: string, title: string) => void;
  onCancelRename: () => void;
  onSubmitRename: (sessionId: string) => void;
  onDeleteSession: (sessionId: string) => void;
  onOpenPasswordDialog: () => void;
  onClosePasswordDialog: () => void;
  onLogout: () => void;
  onNewPasswordChange: (value: string) => void;
  onConfirmPasswordChange: (value: string) => void;
  onResetPassword: () => void;
};

function EyeIcon(props: { open: boolean }) {
  return props.open ? (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 5c5.2 0 8.7 4.5 9.7 6-.9 1.5-4.5 6-9.7 6s-8.7-4.5-9.7-6c.9-1.5 4.5-6 9.7-6Zm0 2C8.8 7 6.2 9.3 4.7 11c1.5 1.7 4.1 4 7.3 4s5.8-2.3 7.3-4C17.8 9.3 15.2 7 12 7Zm0 1.2A2.8 2.8 0 1 1 12 13.8 2.8 2.8 0 0 1 12 8.2Z" />
    </svg>
  ) : (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m4.3 3 16.7 16.7-1.3 1.3-3.1-3.1A10 10 0 0 1 12 19c-5.2 0-8.7-4.5-9.7-6a18 18 0 0 1 4.1-4.5L3 4.3 4.3 3Zm3.5 6.8A14 14 0 0 0 4.7 13c1.5 1.7 4.1 4 7.3 4 1.1 0 2.2-.3 3.1-.8l-1.7-1.7A2.8 2.8 0 0 1 10.5 11.6L7.8 9.8ZM12 7c-.7 0-1.3.1-1.9.3L8.5 5.7A10 10 0 0 1 12 5c5.2 0 8.7 4.5 9.7 6a16 16 0 0 1-3.2 3.7l-1.4-1.4a14 14 0 0 0 2.2-2.3C17.8 9.3 15.2 7 12 7Z" />
    </svg>
  );
}

export function SessionSidebar(props: SessionSidebarProps) {
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  return (
    <aside className={`sidebar ${props.sidebarOpen ? "open" : ""}`}>
      <div className="brand-card">
        <div className="brand-mark">i</div>
        <div>
          <p className="eyebrow">Knowledge Copilot</p>
          <h1>inote</h1>
        </div>
      </div>

      <div className="user-badge">
        <strong>{props.authUsername}</strong>
        <span>只访问自己的知识和会话</span>
        <div className="user-actions">
          <button className="text-button" onClick={props.onOpenPasswordDialog}>
            修改密码
          </button>
          <button className="text-button" onClick={props.onLogout}>
            退出登录
          </button>
        </div>
      </div>

      {props.passwordDialogOpen ? (
        <section className="password-card sidebar-password-card">
          <div className="panel-header">
            <span>修改密码</span>
            <button className="text-button" onClick={props.onClosePasswordDialog}>
              关闭
            </button>
          </div>
          <div className="password-grid">
            <label className="password-input-wrap">
              <input
                type={showNewPassword ? "text" : "password"}
                value={props.newPassword}
                onChange={(event) => props.onNewPasswordChange(event.target.value)}
                placeholder="输入新密码"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowNewPassword((value) => !value)}
                aria-label={showNewPassword ? "隐藏新密码" : "显示新密码"}
              >
                <EyeIcon open={showNewPassword} />
              </button>
            </label>
            <label className="password-input-wrap">
              <input
                type={showConfirmPassword ? "text" : "password"}
                value={props.confirmPassword}
                onChange={(event) => props.onConfirmPasswordChange(event.target.value)}
                placeholder="确认新密码"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowConfirmPassword((value) => !value)}
                aria-label={showConfirmPassword ? "隐藏确认密码" : "显示确认密码"}
              >
                <EyeIcon open={showConfirmPassword} />
              </button>
            </label>
            <button className="primary-button" onClick={props.onResetPassword} disabled={props.passwordSubmitting}>
              {props.passwordSubmitting ? "提交中" : "提交"}
            </button>
          </div>
        </section>
      ) : null}

      <button className="primary-button" onClick={props.onNewSession}>
        新建对话
      </button>

      <div className="search-box">
        <input
          value={props.sessionQuery}
          onChange={(event) => props.onSessionQueryChange(event.target.value)}
          placeholder="搜索会话标题"
        />
      </div>

      <div className="panel-header">
        <span>会话列表</span>
        <span>{props.filteredSessions.length}</span>
      </div>

      <div className="session-list">
        {props.filteredSessions.length === 0 ? (
          <div className="empty-tile compact">
            {props.sessionQuery ? "没有匹配的会话。" : "还没有会话，先发起第一轮问答。"}
          </div>
        ) : (
          props.filteredSessions.map((session) => {
            const pinned = props.pinnedSessionIds.includes(session.id);
            return (
              <article
                key={session.id}
                className={`session-card ${session.id === props.selectedSessionId ? "active" : ""}`}
              >
                {props.renamingId === session.id ? (
                  <>
                    <input
                      className="ghost-input"
                      value={props.renameDraft}
                      onChange={(event) => props.onRenameDraftChange(event.target.value)}
                      onKeyDown={(event) => {
                        if (event.key === "Enter") props.onSubmitRename(session.id);
                        if (event.key === "Escape") props.onCancelRename();
                      }}
                      autoFocus
                    />
                    <div className="session-actions">
                      <button onClick={() => props.onSubmitRename(session.id)}>保存</button>
                      <button onClick={props.onCancelRename}>取消</button>
                    </div>
                  </>
                ) : (
                  <>
                    <button className="session-main" onClick={() => props.onSelectSession(session.id)}>
                      <div className="session-title-row">
                        <strong>{session.title || "未命名会话"}</strong>
                        {pinned ? <span className="pin-badge">已固定</span> : null}
                      </div>
                      <span>{session.messageCount} 条消息</span>
                      <time>{formatTime(session.updatedAt)}</time>
                    </button>
                    <div className="session-actions">
                      <button onClick={() => props.onTogglePinSession(session.id)}>
                        {pinned ? "取消固定" : "固定"}
                      </button>
                      <button onClick={() => props.onStartRename(session.id, session.title)}>重命名</button>
                      <button onClick={() => props.onDeleteSession(session.id)}>删除</button>
                    </div>
                  </>
                )}
              </article>
            );
          })
        )}
      </div>
    </aside>
  );
}
