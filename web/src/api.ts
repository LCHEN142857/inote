import type {
  ChatSession,
  ChatSessionSummary,
  DocumentStatus,
  DocumentUploadResponse,
  InoteResponse
} from "./types";

async function request<T>(input: string, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || "Request failed");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
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
      body: formData
    });

    if (!response.ok) {
      const message = await response.text();
      throw new Error(message || "Upload failed");
    }

    return (await response.json()) as DocumentUploadResponse;
  }
};
