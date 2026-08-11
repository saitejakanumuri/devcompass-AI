export interface QueryRequest {
  question: string;
  provider?: string;
  topK?: number;
  temperature?: number;
  systemPromptOverride?: string;
}

export interface Citation {
  id: string;
  sourceTitle: string;
  sourceType: string;
  sourceUrl?: string;
  excerpt: string;
  relevanceScore: number;
  metadata?: Record<string, any>;
}

export interface Chunk {
  id: string;
  documentId: string;
  documentTitle: string;
  sourceType: string;
  content: string;
  tokenCount: number;
  metadata?: Record<string, any>;
  score: number;
}

export interface QueryResponse {
  question: string;
  answer: string;
  providerUsed: string;
  citations: Citation[];
  retrievedChunks: Chunk[];
  latencyMs: number;
  promptTokens: number;
  completionTokens: number;
}

export interface ProviderConfig {
  id: string;
  name: string;
  model: string;
  providerType: string;
  active: boolean;
  description: string;
  contextWindow: number;
  defaultTemperature: number;
}

export interface PipelineStatus {
  totalChunksIndexed: number;
  pipelineStatus: string;
  activeEmbeddingProvider: string;
  activeVectorDatabase: string;
  rdsInitialized: boolean;
  activeLLMAnswerGenerator: string;
  dimension: number;
}

export interface IngestionResult {
  sourceName: string;
  sourceType: string;
  documentCount: number;
  chunkCount: number;
  status: string;
  elapsedMs: number;
  errorMessage?: string;
}
