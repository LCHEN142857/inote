import { useState } from "react";
import type { FormEvent } from "react";
import type { AuthCaptcha } from "../types";

type AuthViewProps = {
  captcha: AuthCaptcha | null;
  loading: boolean;
  submitting: boolean;
  error: string;
  loginLockRemaining: number;
  onRefresh: () => void;
  onSubmit: (payload: { username: string; password: string; captchaCode: string }) => void;
};

export function AuthView(props: AuthViewProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [captchaCode, setCaptchaCode] = useState("");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    props.onSubmit({ username, password, captchaCode });
  }

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <div className="auth-intro">
          <p className="eyebrow">Knowledge Copilot</p>
          <h1>Bring knowledge, documents, and Q&A into one workspace</h1>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <label className="auth-field">
            <span>用户名</span>
            <input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />
          </label>

          <label className="auth-field">
            <span>密码</span>
            <input
              autoComplete="current-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </label>

          <label className="auth-field">
            <span>验证码</span>
            <div className="captcha-row">
              <input
                className="captcha-input"
                autoComplete="one-time-code"
                inputMode="text"
                maxLength={4}
                value={captchaCode}
                onChange={(e) => setCaptchaCode(e.target.value)}
              />
              <button
                className="captcha-preview"
                type="button"
                onClick={props.onRefresh}
                disabled={props.loading}
                aria-label="刷新验证码"
              >
                {props.captcha?.captchaImage ? (
                  <img
                    className="captcha-image"
                    src={props.captcha.captchaImage}
                    alt="验证码图片"
                    draggable={false}
                  />
                ) : (
                  <div className="captcha-placeholder">{props.loading ? "加载中" : "等待刷新"}</div>
                )}
              </button>
            </div>
          </label>

          {props.error ? <div className="error-banner auth-error">{props.error}</div> : null}
          {props.loginLockRemaining > 0 ? (
            <div className="error-banner auth-error">登录已锁定，请等待 {props.loginLockRemaining} 秒</div>
          ) : null}

          <button
            className="primary-button"
            type="submit"
            disabled={props.submitting || props.loading || !props.captcha || props.loginLockRemaining > 0}
          >
            {props.loginLockRemaining > 0
              ? `锁定中 ${props.loginLockRemaining}s`
              : props.submitting
                ? "提交中"
                : "登录 / 注册"}
          </button>
          <button className="auth-forgot-button text-button" type="button">
            忘记密码
          </button>
        </form>
      </div>
    </div>
  );
}
