// 说明当前这行代码的作用。
import type {
  // 说明当前这行代码的作用。
  AuthCaptcha,
  // 说明当前这行代码的作用。
  AuthResponse,
  // 说明当前这行代码的作用。
  ChatSession,
  // 说明当前这行代码的作用。
  ChatSessionSummary,
  // 说明当前这行代码的作用。
  DocumentStatus,
  // 说明当前这行代码的作用。
  DocumentUploadResponse,
  // 说明当前这行代码的作用。
  InoteResponse
// 说明当前这行代码的作用。
} from "./types";

// 说明当前这行代码的作用。
let authToken = window.localStorage.getItem("inote-auth-token") ?? "";

// 说明当前这行代码的作用。
function buildHeaders(init?: RequestInit) {
  // 说明当前这行代码的作用。
  const headers = new Headers(init?.headers ?? {});
  // 说明当前这行代码的作用。
  if (!headers.has("Content-Type") && !(init?.body instanceof FormData)) {
    // 说明当前这行代码的作用。
    headers.set("Content-Type", "application/json");
  // 说明当前这行代码的作用。
  }
  // 说明当前这行代码的作用。
  if (authToken) {
    // 说明当前这行代码的作用。
    headers.set("X-Auth-Token", authToken);
  // 说明当前这行代码的作用。
  }
  // 说明当前这行代码的作用。
  return headers;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
async function request<T>(input: string, init?: RequestInit): Promise<T> {
  // 说明当前这行代码的作用。
  const response = await fetch(input, {
    // 说明当前这行代码的作用。
    headers: buildHeaders(init),
    // 说明当前这行代码的作用。
    ...init
  // 说明当前这行代码的作用。
  });

  // 说明当前这行代码的作用。
  if (!response.ok) {
    // 说明当前这行代码的作用。
    let message = "";
    // 说明当前这行代码的作用。
    try {
      // 说明当前这行代码的作用。
      const data = (await response.json()) as { error?: string; message?: string };
      // 说明当前这行代码的作用。
      message = data.error || data.message || JSON.stringify(data);
    // 说明当前这行代码的作用。
    } catch {
      // 说明当前这行代码的作用。
      message = await response.text();
    // 说明当前这行代码的作用。
    }
    // 说明当前这行代码的作用。
    throw new Error(message || "Request failed");
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  if (response.status === 204) {
    // 说明当前这行代码的作用。
    return undefined as T;
  // 说明当前这行代码的作用。
  }

  // 说明当前这行代码的作用。
  return (await response.json()) as T;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export const api = {
  // 说明当前这行代码的作用。
  setToken: (token: string) => {
    // 说明当前这行代码的作用。
    authToken = token;
    // 说明当前这行代码的作用。
    if (token) {
      // 说明当前这行代码的作用。
      window.localStorage.setItem("inote-auth-token", token);
    // 说明当前这行代码的作用。
    } else {
      // 说明当前这行代码的作用。
      window.localStorage.removeItem("inote-auth-token");
    // 说明当前这行代码的作用。
    }
  // 说明当前这行代码的作用。
  },
  // 说明当前这行代码的作用。
  getCaptcha: () => request<AuthCaptcha>("/api/v1/auth/captcha"),
  // 说明当前这行代码的作用。
  login: (payload: {
    // 说明当前这行代码的作用。
    username: string;
    // 说明当前这行代码的作用。
    password: string;
    // 说明当前这行代码的作用。
    captchaId: string;
    // 说明当前这行代码的作用。
    captchaCode: string;
  // 说明当前这行代码的作用。
  }) =>
    // 说明当前这行代码的作用。
    request<AuthResponse>("/api/v1/auth/login", {
      // 说明当前这行代码的作用。
      method: "POST",
      // 说明当前这行代码的作用。
      body: JSON.stringify(payload)
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  me: () => request<AuthResponse>("/api/v1/auth/me"),
  // 说明当前这行代码的作用。
  resetPassword: (newPassword: string, confirmPassword: string) =>
    // 说明当前这行代码的作用。
    request<{ message: string }>("/api/v1/auth/password/reset", {
      // 说明当前这行代码的作用。
      method: "POST",
      // 说明当前这行代码的作用。
      body: JSON.stringify({ newPassword, confirmPassword })
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  listSessions: () => request<ChatSessionSummary[]>("/api/v1/chat/sessions"),
  // 说明当前这行代码的作用。
  getSession: (sessionId: string) =>
    // 说明当前这行代码的作用。
    request<ChatSession>(`/api/v1/chat/sessions/${sessionId}`),
  // 说明当前这行代码的作用。
  createSession: (title?: string) =>
    // 说明当前这行代码的作用。
    request<ChatSession>("/api/v1/chat/sessions", {
      // 说明当前这行代码的作用。
      method: "POST",
      // 说明当前这行代码的作用。
      body: JSON.stringify(title ? { title } : {})
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  updateSession: (sessionId: string, title: string) =>
    // 说明当前这行代码的作用。
    request<ChatSession>(`/api/v1/chat/sessions/${sessionId}`, {
      // 说明当前这行代码的作用。
      method: "PUT",
      // 说明当前这行代码的作用。
      body: JSON.stringify({ title })
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  deleteSession: (sessionId: string) =>
    // 说明当前这行代码的作用。
    request<void>(`/api/v1/chat/sessions/${sessionId}`, {
      // 说明当前这行代码的作用。
      method: "DELETE"
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  query: (sessionId: string | undefined, question: string) =>
    // 说明当前这行代码的作用。
    request<InoteResponse>("/api/v1/chat/query", {
      // 说明当前这行代码的作用。
      method: "POST",
      // 说明当前这行代码的作用。
      body: JSON.stringify({ sessionId, question })
    // 说明当前这行代码的作用。
    }),
  // 说明当前这行代码的作用。
  listDocuments: () => request<DocumentStatus[]>("/api/v1/documents"),
  // 说明当前这行代码的作用。
  uploadDocument: async (file: File): Promise<DocumentUploadResponse> => {
    // 说明当前这行代码的作用。
    const formData = new FormData();
    // 说明当前这行代码的作用。
    formData.append("file", file);

    // 说明当前这行代码的作用。
    const response = await fetch("/api/v1/documents/upload", {
      // 说明当前这行代码的作用。
      method: "POST",
      // 说明当前这行代码的作用。
      headers: buildHeaders({ body: formData }),
      // 说明当前这行代码的作用。
      body: formData
    // 说明当前这行代码的作用。
    });

    // 说明当前这行代码的作用。
    if (!response.ok) {
      // 说明当前这行代码的作用。
      let message = "";
      // 说明当前这行代码的作用。
      try {
        // 说明当前这行代码的作用。
        const data = (await response.json()) as { error?: string; message?: string };
        // 说明当前这行代码的作用。
        message = data.error || data.message || JSON.stringify(data);
      // 说明当前这行代码的作用。
      } catch {
        // 说明当前这行代码的作用。
        message = await response.text();
      // 说明当前这行代码的作用。
      }
      // 说明当前这行代码的作用。
      throw new Error(message || "Upload failed");
    // 说明当前这行代码的作用。
    }

    // 说明当前这行代码的作用。
    return (await response.json()) as DocumentUploadResponse;
  // 说明当前这行代码的作用。
  }
// 说明当前这行代码的作用。
};
