import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import type { ProviderConfig, PipelineStatus, QueryResponse, AuthResponse } from './types/api';
import { type TabType } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { QueryCard } from './components/QueryCard';
import { AnswerCard } from './components/AnswerCard';
import { KnowledgeSourcesTab } from './components/KnowledgeSourcesTab';
import { SchemaExplorerTab } from './components/SchemaExplorerTab';
import { ArchitectureTab } from './components/ArchitectureTab';
import { OnboardingTab } from './components/OnboardingTab';
import { AuthModal } from './components/AuthModal';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabType>('query');
  const [authModalOpen, setAuthModalOpen] = useState(false);
  const [currentUser, setCurrentUser] = useState<AuthResponse | null>(null);

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

  const [selectedProvider, setSelectedProvider] = useState('Google Gemini');
  const [pipelineStatus, setPipelineStatus] = useState<PipelineStatus | null>(null);
  const [queryResponse, setQueryResponse] = useState<QueryResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const user = api.getStoredUser();
    if (user) setCurrentUser(user);
    loadInitialData();
  }, []);

  const loadInitialData = async () => {
    try {
      const statusData = await api.getPipelineStatus();
      setPipelineStatus(statusData);
    } catch (err) {
      console.warn('Backend connection pending or offline.');
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
        active: (p.name || p.id || p.providerType).toLowerCase() === providerKey.toLowerCase(),
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

  const handleLogout = () => {
    api.logout();
    setCurrentUser(null);
  };

  return (
    <div className="app-layout">
      {/* Left Navigation Sidebar (ChatGPT / Claude Style) */}
      <Sidebar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        currentUser={currentUser}
        status={pipelineStatus}
        onOpenAuth={() => setAuthModalOpen(true)}
        onLogout={handleLogout}
        onNewChat={() => {
          setQueryResponse(null);
          setErrorMessage(null);
        }}
      />

      {/* Main Content Area with Sticky Header */}
      <div className="app-main-wrapper">
        <TopBar
          activeTab={activeTab}
          currentUser={currentUser}
          providers={providers}
          selectedProvider={selectedProvider}
          onSelectProvider={handleSelectProvider}
          onOpenAuth={() => setAuthModalOpen(true)}
          onLogout={handleLogout}
        />

        <main className="main-content-area">
          {activeTab === 'query' && (
            <div className="space-y-6 max-w-4xl mx-auto">
              <div className="hero text-center py-2">
                <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">Internal Architecture & Knowledge AI</h1>
                <p className="text-sm text-slate-600 mt-1">
                  Query system flows, Notion docs, git repos, or database schemas via AI-powered RAG.
                </p>
              </div>

              <QueryCard
                onExecuteQuery={handleExecuteQuery}
                isLoading={isLoading}
              />

              {errorMessage && (
                <div className="error-banner p-4 bg-red-50 border border-red-200 text-red-800 rounded-xl text-sm font-medium">
                  ⚠️ {errorMessage}
                </div>
              )}

              <AnswerCard
                response={queryResponse}
                isLoading={isLoading}
              />
            </div>
          )}

          {activeTab === 'sources' && <KnowledgeSourcesTab />}
          {activeTab === 'schema' && <SchemaExplorerTab />}
          {activeTab === 'architecture' && <ArchitectureTab />}
          {activeTab === 'onboarding' && <OnboardingTab />}
        </main>
      </div>

      <AuthModal
        isOpen={authModalOpen}
        onClose={() => setAuthModalOpen(false)}
        onSuccess={(user) => setCurrentUser(user)}
      />
    </div>
  );
};

export default App;
