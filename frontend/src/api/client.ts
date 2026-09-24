import axios from 'axios';
import type {
  QueryRequest,
  QueryResponse,
  IngestionResult,
  AuthResponse,
  AccountKnowledgeConfig,
} from '../types/api';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('devcompass_auth_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('devcompass_auth_token');
      localStorage.removeItem('devcompass_user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const api = {
  // Authentication API
  async login(payload: { email: string; password?: string }): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', payload);
    if (response.data.token) {
      localStorage.setItem('devcompass_auth_token', response.data.token);
      localStorage.setItem('devcompass_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  async register(payload: { email: string; password?: string; fullName?: string }): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/register', payload);
    if (response.data.token) {
      localStorage.setItem('devcompass_auth_token', response.data.token);
      localStorage.setItem('devcompass_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  logout() {
    localStorage.removeItem('devcompass_auth_token');
    localStorage.removeItem('devcompass_user');
    window.location.href = '/login';
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

  // Account Knowledge Configurations
  async getAccountConfigs(): Promise<AccountKnowledgeConfig[]> {
    const response = await apiClient.get<AccountKnowledgeConfig[]>('/sources');
    return response.data;
  },

  async saveAccountConfig(sourceType: string, configJson: Record<string, any>, autoSync = false): Promise<any> {
    const response = await apiClient.post(`/sources/${sourceType}?autoSync=${autoSync}`, configJson);
    return response.data;
  },

  async syncAccountSource(sourceType: string): Promise<IngestionResult> {
    const response = await apiClient.post<IngestionResult>(`/sources/${sourceType}/sync`);
    return response.data;
  }
};
