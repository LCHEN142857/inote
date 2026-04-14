export interface SourceReference {
  fileName: string;
  url: string;
}

export interface InoteResponse {
  sessionId: string;
  answer: string;
  sources: SourceReference[];
}

export interface ChatMessage {
  id: string;
  role: string;
  content: string;
  createdAt: string;
}

export interface ChatSession {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessage[];
}

export interface ChatSessionSummary {
  id: string;
  title: string;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentStatus {
  documentId: string;
  fileName: string;
  fileSize: number;
  status: string;
  errorMessage?: string | null;
  updatedAt: string;
}

export interface DocumentUploadResponse {
  documentId: string;
  fileName: string;
  status: string;
  message: string;
}

export interface LocalChatMessage extends ChatMessage {
  sources?: SourceReference[];
  pending?: boolean;
}
