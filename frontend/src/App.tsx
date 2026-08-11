import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import type { ProviderConfig, PipelineStatus, QueryResponse } from './types/api';
import { Header } from './components/Header';
import { ProviderSelector } from './components/ProviderSelector';
import { QueryCard } from './components/QueryCard';
import { AnswerCard } from './components/AnswerCard';

export const App: React.FC = () => {
  const [providers, setProviders] = useState<ProviderConfig[]>([
    {
      id: 'gemini',
      name: 'Google Gemini',
      model: 'gemini-1.5-flash',
      providerType: 'gemini',
      active: true,
      description: 'Google Gemini LLM',
      contextWindow: 1000000,
      defaultTemperature: 0.2,
    },
    {
      id: 'openai',
      name: 'OpenAI GPT-4o',
      model: 'gpt-4o',
      providerType: 'openai',
      active: false,
      description: 'OpenAI GPT-4o',
      contextWindow: 128000,
      defaultTemperature: 0.2,
    },
    {
      id: 'claude',
      name: 'Anthropic Claude',
      model: 'claude-3.5-sonnet',
      providerType: 'claude',
      active: false,
      description: 'Anthropic Claude 3.5',
      contextWindow: 200000,
      defaultTemperature: 0.2,
    },
    {
      id: 'ollama',
      name: 'Ollama (Local)',
      model: 'llama3',
      providerType: 'ollama',
      active: false,
      description: 'Ollama Local LLM',
      contextWindow: 8192,
      defaultTemperature: 0.2,
    },
  ]);

  const [selectedProvider, setSelectedProvider] = useState('Gemini');
  const [pipelineStatus, setPipelineStatus] = useState<PipelineStatus | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [queryResponse, setQueryResponse] = useState<QueryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isSyncing, setIsSyncing] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    try {
      const statusData = await api.getPipelineStatus();
      setPipelineStatus(statusData);
      setIsConnected(true);
    } catch (err) {
      console.warn('Backend connection pending or offline.');
      setIsConnected(false);
    }

    try {
      const providerData = await api.getProviders();
      if (providerData && providerData.length > 0) {
        setProviders(providerData);
        const active = providerData.find((p) => p.active);
        if (active) {
          setSelectedProvider(active.name || active.id);
        }
      }
    } catch (err) {
      console.warn('Failed to load dynamic providers.');
    }
  };

  const handleSelectProvider = async (providerKey: string) => {
    setSelectedProvider(providerKey);
    setProviders((prev) =>
      prev.map((p) => ({
        ...p,
        active: (p.id || p.providerType || p.name).toLowerCase() === providerKey.toLowerCase(),
      }))
    );

    try {
      await api.setActiveProvider(providerKey);
    } catch (err) {
      console.log('Provider selected locally');
    }
  };

  const handleExecuteQuery = async (
    question: string,
    topK: number,
    temperature: number
  ) => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const result = await api.executeQuery({
        question,
        provider: selectedProvider,
        topK,
        temperature,
      });
      setQueryResponse(result);
    } catch (err: any) {
      console.error('Query execution error:', err);
      setErrorMessage(
        err.response?.data?.message || err.message || 'Failed to query system.'
      );
      setQueryResponse(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSync = async () => {
    setIsSyncing(true);
    try {
      const results = await api.runFullIngestion();
      alert(`Knowledge sync completed! Processed ${results.length} sources.`);
      await loadInitialData();
    } catch (err: any) {
      alert('Knowledge sync request triggered.');
    } finally {
      setIsSyncing(false);
    }
  };

  return (
    <div className="app-layout">
      <Header
        status={pipelineStatus}
        isConnected={isConnected}
        isSyncing={isSyncing}
        onSync={handleSync}
      />

      <main className="main-content">
        <div className="hero">
          <h1>Internal Architecture & Knowledge AI</h1>
          <p>
            Query system flows, Notion docs, git repos, or database schemas via AI-powered RAG.
          </p>
        </div>

        <ProviderSelector
          providers={providers}
          selectedProvider={selectedProvider}
          onSelectProvider={handleSelectProvider}
        />

        <QueryCard
          onExecuteQuery={handleExecuteQuery}
          isLoading={isLoading}
        />

        {errorMessage && (
          <div className="error-banner">
            ⚠️ {errorMessage}
          </div>
        )}

        <AnswerCard
          response={queryResponse}
          isLoading={isLoading}
        />
      </main>

      <footer className="footer">
        <p>
          DevCompass AI Platform &bull; React + TypeScript Frontend &bull; Spring Boot + pgvector
        </p>
      </footer>
    </div>
  );
};

export default App;
