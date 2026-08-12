import axios from 'axios';
import type {
  QueryRequest,
  QueryResponse,
  ProviderConfig,
  PipelineStatus,
  IngestionResult,
  AuthResponse,
  AccountKnowledgeConfig,
  GitRepoConfig,
  DatabaseConnectionRequest,
  SchemaMetadata,
  KnowledgeSourceInfo,
  ArchitectureDetails,
  OnboardingFlow,
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor to add auth token, account ID, and user ID headers
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('devcompass_auth_token');
  const accountId = localStorage.getItem('devcompass_account_id');
  const userId = localStorage.getItem('devcompass_user_id');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  if (accountId) {
    config.headers['X-Account-Id'] = accountId;
  }
  if (userId) {
    config.headers['X-User-Id'] = userId;
  }
  return config;
});

export const api = {
  // Authentication API
  async login(payload: { email: string; password?: string }): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', payload);
    if (response.data.token) {
      localStorage.setItem('devcompass_auth_token', response.data.token);
      localStorage.setItem('devcompass_account_id', response.data.accountId);
      localStorage.setItem('devcompass_user_id', response.data.userId);
      localStorage.setItem('devcompass_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  async register(payload: { companyName?: string; email: string; password?: string; fullName?: string }): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/register', payload);
    if (response.data.token) {
      localStorage.setItem('devcompass_auth_token', response.data.token);
      localStorage.setItem('devcompass_account_id', response.data.accountId);
      localStorage.setItem('devcompass_user_id', response.data.userId);
      localStorage.setItem('devcompass_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  logout() {
    localStorage.removeItem('devcompass_auth_token');
    localStorage.removeItem('devcompass_account_id');
    localStorage.removeItem('devcompass_user_id');
    localStorage.removeItem('devcompass_user');
  },

  getStoredUser(): AuthResponse | null {
    const userStr = localStorage.getItem('devcompass_user');
    if (!userStr) return null;
    try {
      return JSON.parse(userStr);
    } catch {
      return null;
    }
  },

  // Query System Engine
  async executeQuery(payload: QueryRequest): Promise<QueryResponse> {
    const response = await apiClient.post<QueryResponse>('/query', payload);
    return response.data;
  },

  // AI Providers API
  async getProviders(): Promise<ProviderConfig[]> {
    const response = await apiClient.get<ProviderConfig[]>('/providers');
    return response.data;
  },

  async setActiveProvider(providerKey: string): Promise<{ status: string; activeProvider: string }> {
    const response = await apiClient.post('/providers/active', { provider: providerKey });
    return response.data;
  },

  // Pipeline Telemetry & Health & Architecture
  async getPipelineStatus(): Promise<PipelineStatus> {
    const response = await apiClient.get<PipelineStatus>('/pipeline/status');
    return response.data;
  },

  async getArchitecture(): Promise<ArchitectureDetails> {
    const response = await apiClient.get<ArchitectureDetails>('/pipeline/architecture');
    return response.data;
  },

  async runFullIngestion(): Promise<IngestionResult[]> {
    const response = await apiClient.post<IngestionResult[]>('/pipeline/ingest');
    return response.data;
  },

  // Account Knowledge Configurations
  async getAccountConfigs(): Promise<AccountKnowledgeConfig[]> {
    const response = await apiClient.get<AccountKnowledgeConfig[]>('/account/configs');
    return response.data;
  },

  async saveAccountConfig(sourceType: string, configJson: Record<string, any>, autoSync = false): Promise<any> {
    const response = await apiClient.post(`/account/configs/${sourceType}?autoSync=${autoSync}`, configJson);
    return response.data;
  },

  async syncAccountSource(sourceType: string): Promise<IngestionResult> {
    const response = await apiClient.post<IngestionResult>(`/account/sync/${sourceType}`);
    return response.data;
  },

  // Git Repository API
  async getGitConfig(): Promise<GitRepoConfig & { status?: string }> {
    const response = await apiClient.get('/git/config');
    return response.data;
  },

  async updateGitConfig(config: GitRepoConfig, autoSync = false): Promise<any> {
    const response = await apiClient.post(`/git/config?autoSync=${autoSync}`, config);
    return response.data;
  },

  async syncGitRepo(): Promise<IngestionResult> {
    const response = await apiClient.post<IngestionResult>('/git/sync');
    return response.data;
  },

  // Knowledge Sources Status API
  async getSources(): Promise<KnowledgeSourceInfo[]> {
    const response = await apiClient.get<KnowledgeSourceInfo[]>('/sources');
    return response.data;
  },

  async syncSource(sourceType: string): Promise<IngestionResult> {
    const response = await apiClient.post<IngestionResult>(`/sources/sync/${sourceType}`);
    return response.data;
  },

  // Schema Explorer API
  async getTables(): Promise<SchemaMetadata[]> {
    const response = await apiClient.get<SchemaMetadata[]>('/schema/tables');
    return response.data;
  },

  async getTableSchema(tableName: string): Promise<SchemaMetadata> {
    const response = await apiClient.get<SchemaMetadata>(`/schema/tables/${tableName}`);
    return response.data;
  },

  async testDbConnection(payload: DatabaseConnectionRequest): Promise<{ status: string; message: string; url: string }> {
    const response = await apiClient.post<{ status: string; message: string; url: string }>('/schema/test-connection', payload);
    return response.data;
  },

  async syncDbSchema(): Promise<IngestionResult> {
    const response = await apiClient.post<IngestionResult>('/schema/sync');
    return response.data;
  },

  // Onboarding Flows API
  async getOnboardingFlows(): Promise<OnboardingFlow[]> {
    const response = await apiClient.get<OnboardingFlow[]>('/onboarding/flows');
    return response.data;
  },
};
