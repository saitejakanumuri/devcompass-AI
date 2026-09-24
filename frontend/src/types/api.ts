export interface AuthResponse {
  token: string | null;
  userId: string;
  fullName: string;
  email: string;
  role: string;
}

export interface IngestionResult {
  sourceType: string;
  documentsProcessed: number;
  chunksCreated: number;
  embeddingsGenerated: number;
  status: string;
  logs: string[];
  timestamp: string;
}

export interface AccountKnowledgeConfig {
  id: string;
  userId: string;
  sourceType: string;
  configJson: Record<string, any>;
  status: string;
  lastSyncedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Citation {
  id: string;
  documentTitle: string;
  sourceType: string;
  documentId: string;
  snippet: string;
  relevanceScore: number;
  metadata: Record<string, any>;
}

export interface Chunk {
  id: string;
  documentId: string;
  documentTitle: string;
  sourceType: string;
  content: string;
  score: number;
}

export interface QueryRequest {
  question: string;
  topK?: number;
  provider: string;
}

export interface QueryResponse {
  originalQuestion: string;
  answer: string;
  providerUsed: string;
  citations: Citation[];
  retrievedChunks: Chunk[];
  latencyMs: number;
}
