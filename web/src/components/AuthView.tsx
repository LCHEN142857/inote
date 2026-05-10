import { useState } from "react";
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

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <p className="eyebrow">User Access</p>
        <h1>登录或注册</h1>
        <p className="auth-copy">第一次登录自动注册，后续登录校验用户名、密码和验证码。</p>

        <label className="auth-field">
          <span>用户名</span>
          <input autoComplete="username" value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>密码</span>
          <input autoComplete="current-password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>验证码</span>
          <div className="captcha-row">
            <input
              autoComplete="one-time-code"
              inputMode="text"
              maxLength={4}
              value={captchaCode}
              onChange={(e) => setCaptchaCode(e.target.value)}
            />
            <div className="captcha-preview">
              {props.captcha?.captchaImage ? (
                <img className="captcha-image" src={props.captcha.captchaImage} alt="验证码图片" draggable={false} />
              ) : (
                <div className="captcha-placeholder">{props.loading ? "加载中" : "等待刷新"}</div>
              )}
              <button className="captcha-refresh" type="button" onClick={props.onRefresh} disabled={props.loading}>
                换一张
              </button>
            </div>
          </div>
        </label>

        {props.error ? <div className="error-banner auth-error">{props.error}</div> : null}
        {props.loginLockRemaining > 0 ? (
          <div className="error-banner auth-error">登录已锁定，请等待 {props.loginLockRemaining} 秒</div>
        ) : null}

        <button
          className="primary-button"
          onClick={() => props.onSubmit({ username, password, captchaCode })}
          disabled={props.submitting || props.loading || !props.captcha || props.loginLockRemaining > 0}
        >
          {props.loginLockRemaining > 0
            ? `锁定中 ${props.loginLockRemaining}s`
            : props.submitting
              ? "提交中"
              : "登录 / 注册"}
        </button>
      </div>
    </div>
  );
}
