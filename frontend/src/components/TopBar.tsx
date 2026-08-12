import React from 'react';
import { ProviderSelector } from './ProviderSelector';
import type { ProviderConfig, AuthResponse } from '../types/api';
import type { TabType } from './Header';
import { Sparkles, GitBranch, Database, Cpu, BookOpen } from 'lucide-react';

interface TopBarProps {
  activeTab: TabType;
  currentUser: AuthResponse | null;
  providers: ProviderConfig[];
  selectedProvider: string;
  onSelectProvider: (providerKey: string) => void;
  onOpenAuth: () => void;
  onLogout: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  activeTab,
  providers,
  selectedProvider,
  onSelectProvider,
}) => {
  const getTabTitle = () => {
    switch (activeTab) {
      case 'query':       return { label: 'AI Knowledge Query & RAG Chat', Icon: Sparkles };
      case 'sources':     return { label: 'Knowledge Sources Configuration', Icon: GitBranch };
      case 'schema':      return { label: 'Target Schema Explorer', Icon: Database };
      case 'architecture':return { label: 'Architecture & System Topo', Icon: Cpu };
      case 'onboarding':  return { label: 'Platform Guide & Onboarding', Icon: BookOpen };
      default:            return { label: 'DevCompass AI Workspace', Icon: Sparkles };
    }
  };

  const { label, Icon } = getTabTitle();

  return (
    <header className="app-topbar">
      <div className="topbar-left">
        <Icon size={18} className="topbar-icon" />
        <h1 className="topbar-title">{label}</h1>
      </div>
      <div className="topbar-right">
        <ProviderSelector
          providers={providers}
          selectedProvider={selectedProvider}
          onSelectProvider={onSelectProvider}
        />
      </div>
    </header>
  );
};
