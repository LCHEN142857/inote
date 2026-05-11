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

export function SessionSidebar(props: SessionSidebarProps) {
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
