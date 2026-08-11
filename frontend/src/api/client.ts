import axios from 'axios';
import type {
  QueryRequest,
  QueryResponse,
  ProviderConfig,
  PipelineStatus,
  IngestionResult,
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const api = {
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

  // Pipeline Telemetry & Health
  async getPipelineStatus(): Promise<PipelineStatus> {
    const response = await apiClient.get<PipelineStatus>('/pipeline/status');
    return response.data;
  },

  async runFullIngestion(): Promise<IngestionResult[]> {
    const response = await apiClient.post<IngestionResult[]>('/pipeline/ingest');
    return response.data;
  },
};
