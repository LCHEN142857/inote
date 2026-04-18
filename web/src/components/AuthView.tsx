import { useState } from "react";
import type { AuthCaptcha } from "../types";

type AuthViewProps = {
  captcha: AuthCaptcha | null;
  loading: boolean;
  submitting: boolean;
  error: string;
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
          <input value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>密码</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </label>

        <label className="auth-field">
          <span>验证码</span>
          <div className="captcha-row">
            <input value={captchaCode} onChange={(e) => setCaptchaCode(e.target.value)} />
            <button className="captcha-badge" onClick={props.onRefresh} disabled={props.loading}>
              {props.loading ? "加载中" : props.captcha?.captchaCode ?? "刷新"}
            </button>
          </div>
        </label>

        {props.error ? <div className="error-banner auth-error">{props.error}</div> : null}

        <button
          className="primary-button"
          onClick={() => props.onSubmit({ username, password, captchaCode })}
          disabled={props.submitting || props.loading || !props.captcha}
        >
          {props.submitting ? "提交中" : "登录 / 注册"}
        </button>
      </div>
    </div>
  );
}
