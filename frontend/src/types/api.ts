export type SourceType =
  | 'NOTION'
  | 'GIT_REPOSITORY'
  | 'DATABASE_METADATA'
  | 'JIRA'
  | 'CONFLUENCE'
  | 'SLACK';

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
  sourceType: SourceType | string;
  documentPath?: string;
  snippet?: string;
  sourceUrl?: string;
  excerpt?: string;
  relevanceScore: number;
  metadata?: Record<string, any>;
}

export interface Chunk {
  id: string;
  documentId: string;
  documentTitle: string;
  sourceType: SourceType | string;
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
  sourceType: SourceType | string;
  documentsProcessed: number;
  totalChunksGenerated: number;
  vectorEmbeddingsCreated: number;
  status: string;
  logs?: string[];
  timestamp?: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  accountId: string;
  companyName: string;
  fullName: string;
  email: string;
  role: string;
}

export interface AccountKnowledgeConfig {
  id: string;
  accountId: string;
  userId?: string | null;
  sourceType: SourceType | string;
  configJson: Record<string, any>;
  status: string;
  lastSyncedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CompanyUser {
  id: string;
  accountId: string;
  email: string;
  fullName: string;
  role: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateCompanyUserRequest {
  email: string;
  password: string;
  fullName: string;
  role: 'ADMIN' | 'USER' | string;
}

export interface UserOverrideRequest {
  userId: string;
  sourceType: SourceType | string;
  configJson: Record<string, any>;
}

export interface GitRepoConfig {
  repoUrl: string;
  repoPath: string;
  branch: string;
  includedExtensions: string;
  maxFileSizeKb: number;
  status?: string;
}

export interface DatabaseConnectionRequest {
  url: string;
  username: string;
  password?: string;
}

export interface ColumnMetadata {
  name: string;
  dataType: string;
  nullable: boolean;
  description?: string;
}

export interface ForeignKeyMetadata {
  columnName: string;
  targetTable: string;
  targetColumn: string;
}

export interface SchemaMetadata {
  tableSchema: string;
  tableName: string;
  description?: string;
  columns: ColumnMetadata[];
  primaryKeys: string[];
  foreignKeys: ForeignKeyMetadata[];
  vectorIndexed: boolean;
}

export interface KnowledgeSourceInfo {
  type: SourceType | string;
  name: string;
  healthy: boolean;
}

export interface ArchitectureDetails {
  embeddingsEngine: {
    provider: string;
    model: string;
    endpoint: string;
    dimension: number;
    status: string;
  };
  vectorDatabase: {
    database: string;
    tableName: string;
    distanceMetric: string;
    rdsConnected: boolean;
    sampleQuery: string;
  };
  llmAnswerGenerator: {
    provider: string;
    model: string;
    contextWindow: string;
    status: string;
  };
}

export interface OnboardingStep {
  stepNumber: number;
  title: string;
  description: string;
  actionCommand?: string;
}

export interface OnboardingFlow {
  id: string;
  title: string;
  description: string;
  targetRole: string;
  steps: OnboardingStep[];
}
