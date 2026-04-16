// 说明当前这行代码的作用。
export interface SourceReference {
  // 说明当前这行代码的作用。
  fileName: string;
  // 说明当前这行代码的作用。
  url: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface AuthCaptcha {
  // 说明当前这行代码的作用。
  captchaId: string;
  // 说明当前这行代码的作用。
  captchaCode: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface AuthResponse {
  // 说明当前这行代码的作用。
  token: string;
  // 说明当前这行代码的作用。
  username: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface InoteResponse {
  // 说明当前这行代码的作用。
  sessionId: string;
  // 说明当前这行代码的作用。
  answer: string;
  // 说明当前这行代码的作用。
  sources: SourceReference[];
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface ChatMessage {
  // 说明当前这行代码的作用。
  id: string;
  // 说明当前这行代码的作用。
  role: string;
  // 说明当前这行代码的作用。
  content: string;
  // 说明当前这行代码的作用。
  createdAt: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface ChatSession {
  // 说明当前这行代码的作用。
  id: string;
  // 说明当前这行代码的作用。
  title: string;
  // 说明当前这行代码的作用。
  createdAt: string;
  // 说明当前这行代码的作用。
  updatedAt: string;
  // 说明当前这行代码的作用。
  messages: ChatMessage[];
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface ChatSessionSummary {
  // 说明当前这行代码的作用。
  id: string;
  // 说明当前这行代码的作用。
  title: string;
  // 说明当前这行代码的作用。
  messageCount: number;
  // 说明当前这行代码的作用。
  createdAt: string;
  // 说明当前这行代码的作用。
  updatedAt: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface DocumentStatus {
  // 说明当前这行代码的作用。
  documentId: string;
  // 说明当前这行代码的作用。
  fileName: string;
  // 说明当前这行代码的作用。
  fileSize: number;
  // 说明当前这行代码的作用。
  status: string;
  // 说明当前这行代码的作用。
  errorMessage?: string | null;
  // 说明当前这行代码的作用。
  updatedAt: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface DocumentUploadResponse {
  // 说明当前这行代码的作用。
  documentId: string;
  // 说明当前这行代码的作用。
  fileName: string;
  // 说明当前这行代码的作用。
  status: string;
  // 说明当前这行代码的作用。
  message: string;
// 说明当前这行代码的作用。
}

// 说明当前这行代码的作用。
export interface LocalChatMessage extends ChatMessage {
  // 说明当前这行代码的作用。
  sources?: SourceReference[];
  // 说明当前这行代码的作用。
  pending?: boolean;
// 说明当前这行代码的作用。
}
