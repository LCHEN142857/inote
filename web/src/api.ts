import type {
  AuthCaptcha,
  AuthResponse,
  ChatModelCatalog,
  ChatSession,
  ChatSessionSummary,
  ChatStreamHandlers,
  DocumentStatus,
  DocumentUploadResponse,
  InoteResponse,
  UserSettings
} from "./types";

let authToken = window.localStorage.getItem("inote-auth-token") ?? "";
let unauthorizedHandler: (() => void) | null = null;

export class ApiError extends Error {
  status: number;
  details: Record<string, unknown>;

  constructor(message: string, status: number, details: Record<string, unknown>) {
    super(message);
    this.status = status;
    this.details = details;
  }
}

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

function isPublicAuthRequest(input: string) {
  return input === "/api/v1/auth/captcha" || input === "/api/v1/auth/login";
}

async function readErrorBody(response: Response) {
  try {
    return (await response.json()) as Record<string, unknown>;
  } catch {
    return { error: await response.text() };
  }
}

async function readStreamBody(response: Response, handlers: ChatStreamHandlers) {
  if (!response.body) {
    throw new ApiError("Streaming response is not available", response.status, {});
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const dispatchEvent = (rawEvent: string) => {
    const lines = rawEvent.split("\n");
    let eventName = "message";
    const dataLines: string[] = [];

    for (const line of lines) {
      if (line.startsWith("event:")) {
        eventName = line.slice(6).trim();
      } else if (line.startsWith("data:")) {
        dataLines.push(line.slice(5).trimStart());
      }
    }

    if (!dataLines.length) return;
    const data = dataLines.join("\n");
    if (eventName === "delta") {
      handlers.onDelta?.(data);
      return;
    }

    const responseData = JSON.parse(data) as InoteResponse;
    if (eventName === "metadata") {
      handlers.onMetadata?.(responseData);
    } else if (eventName === "done") {
      handlers.onDone?.(responseData);
    }
  };

  while (true) {
    const { value, done } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const events = buffer.split("\n\n");
    buffer = events.pop() ?? "";
    events.forEach(dispatchEvent);
  }

  buffer += decoder.decode();
  if (buffer.trim()) {
    dispatchEvent(buffer);
  }
}

async function request<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    headers: buildHeaders(init),
    ...init
  });

  if (!response.ok) {
    const body = await readErrorBody(response);
    const message = typeof body.error === "string" ? body.error : typeof body.message === "string" ? body.message : "Request failed";
    if (response.status === 401 && authToken && !isPublicAuthRequest(input)) {
      unauthorizedHandler?.();
    }
    throw new ApiError(message, response.status, body);
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
  setUnauthorizedHandler: (handler: (() => void) | null) => {
    unauthorizedHandler = handler;
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
  getSettings: () => request<UserSettings>("/api/v1/auth/settings"),
  getChatModels: () => request<ChatModelCatalog>("/api/v1/chat/models"),
  updateSettings: (answerFromReferencesOnly: boolean) =>
    request<UserSettings>("/api/v1/auth/settings", {
      method: "POST",
      body: JSON.stringify({ answerFromReferencesOnly })
    }),
  resetPassword: (newPassword: string, confirmPassword: string) =>
    request<{ message: string }>("/api/v1/auth/password/reset", {
      method: "POST",
      body: JSON.stringify({ newPassword, confirmPassword })
    }),
  listSessions: () => request<ChatSessionSummary[]>("/api/v1/chat/sessions"),
  getSession: (sessionId: string) => request<ChatSession>(`/api/v1/chat/sessions/${sessionId}`),
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
  query: (sessionId: string | undefined, question: string, model: string) =>
    request<InoteResponse>("/api/v1/chat/query", {
      method: "POST",
      body: JSON.stringify({ sessionId, question, model })
    }),
  queryStream: async (
    sessionId: string | undefined,
    question: string,
    model: string,
    handlers: ChatStreamHandlers
  ) => {
    const response = await fetch("/api/v1/chat/query/stream", {
      method: "POST",
      headers: buildHeaders(),
      body: JSON.stringify({ sessionId, question, model })
    });

    if (!response.ok) {
      const body = await readErrorBody(response);
      const message = typeof body.error === "string" ? body.error : typeof body.message === "string" ? body.message : "Request failed";
      if (response.status === 401 && authToken) {
        unauthorizedHandler?.();
      }
      throw new ApiError(message, response.status, body);
    }

    await readStreamBody(response, handlers);
  },
  listDocuments: () => request<DocumentStatus[]>("/api/v1/documents"),
  deleteDocument: (documentId: string) =>
    request<void>(`/api/v1/documents/delete/${documentId}`, {
      method: "DELETE"
    }),
  uploadDocument: async (file: File): Promise<DocumentUploadResponse> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/v1/documents/upload", {
      method: "POST",
      headers: buildHeaders({ body: formData }),
      body: formData
    });

    if (!response.ok) {
      const body = await readErrorBody(response);
      const message = typeof body.error === "string" ? body.error : typeof body.message === "string" ? body.message : "Upload failed";
      if (response.status === 401 && authToken) {
        unauthorizedHandler?.();
      }
      throw new ApiError(message, response.status, body);
    }

    return (await response.json()) as DocumentUploadResponse;
  }
};
