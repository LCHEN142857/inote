import type {
  AuthCaptcha,
  AuthResponse,
  ChatSession,
  ChatSessionSummary,
  DocumentStatus,
  DocumentUploadResponse,
  InoteResponse
} from "./types";

let authToken = window.localStorage.getItem("inote-auth-token") ?? "";

function buildHeaders(init?: RequestInit) {
  const headers = new Headers(init?.headers ?? {});
  if (!headers.has("Content-Type") && !(init?.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (authToken) {
    headers.set("X-Auth-Token", authToken);
  }
  return headers;
}

async function request<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    headers: buildHeaders(init),
    ...init
  });

  if (!response.ok) {
    let message = "";
    try {
      const data = (await response.json()) as { error?: string; message?: string };
      message = data.error || data.message || JSON.stringify(data);
    } catch {
      message = await response.text();
    }
    throw new Error(message || "Request failed");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  setToken: (token: string) => {
    authToken = token;
    if (token) {
      window.localStorage.setItem("inote-auth-token", token);
    } else {
      window.localStorage.removeItem("inote-auth-token");
    }
  },
  getCaptcha: () => request<AuthCaptcha>("/api/v1/auth/captcha"),
  login: (payload: {
    username: string;
    password: string;
    captchaId: string;
    captchaCode: string;
  }) =>
    request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(payload)
    }),
  me: () => request<AuthResponse>("/api/v1/auth/me"),
  resetPassword: (newPassword: string, confirmPassword: string) =>
    request<{ message: string }>("/api/v1/auth/password/reset", {
      method: "POST",
      body: JSON.stringify({ newPassword, confirmPassword })
    }),
  listSessions: () => request<ChatSessionSummary[]>("/api/v1/chat/sessions"),
  getSession: (sessionId: string) =>
    request<ChatSession>(`/api/v1/chat/sessions/${sessionId}`),
  createSession: (title?: string) =>
    request<ChatSession>("/api/v1/chat/sessions", {
      method: "POST",
      body: JSON.stringify(title ? { title } : {})
    }),
  updateSession: (sessionId: string, title: string) =>
    request<ChatSession>(`/api/v1/chat/sessions/${sessionId}`, {
      method: "PUT",
      body: JSON.stringify({ title })
    }),
  deleteSession: (sessionId: string) =>
    request<void>(`/api/v1/chat/sessions/${sessionId}`, {
      method: "DELETE"
    }),
  query: (sessionId: string | undefined, question: string) =>
    request<InoteResponse>("/api/v1/chat/query", {
      method: "POST",
      body: JSON.stringify({ sessionId, question })
    }),
  listDocuments: () => request<DocumentStatus[]>("/api/v1/documents"),
  uploadDocument: async (file: File): Promise<DocumentUploadResponse> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/v1/documents/upload", {
      method: "POST",
      headers: buildHeaders({ body: formData }),
      body: formData
    });

    if (!response.ok) {
      let message = "";
      try {
        const data = (await response.json()) as { error?: string; message?: string };
        message = data.error || data.message || JSON.stringify(data);
      } catch {
        message = await response.text();
      }
      throw new Error(message || "Upload failed");
    }

    return (await response.json()) as DocumentUploadResponse;
  }
};
