import {
  Compass,
  RefreshCw,
  Search,
  Database,
  GitBranch,
  Cpu,
  BookOpen,
  User,
  LogOut,
} from 'lucide-react';
import type { PipelineStatus, AuthResponse } from '../types/api';

export type TabType = 'query' | 'sources' | 'schema' | 'architecture' | 'onboarding';

interface HeaderProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
  status: PipelineStatus | null;
  isConnected: boolean;
  isSyncing: boolean;
  onSync: () => void;
  user: AuthResponse | null;
  onOpenAuth: () => void;
  onLogout: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  activeTab,
  onTabChange,
  status,
  isConnected,
  isSyncing,
  onSync,
  user,
  onOpenAuth,
  onLogout,
}) => {
  return (
    <header className="header">
      <div className="header-container">
        <a href="#" className="brand">
          <div className="brand-icon">
            <Compass size={22} className="compass-spin" />
          </div>
          <div>
            <span className="brand-title">DevCompass AI</span>
            <span className="brand-badge">RAG Platform</span>
          </div>
        </a>

        {/* Navigation Tabs */}
        <nav className="header-nav">
          <button
            type="button"
            className={`nav-tab ${activeTab === 'query' ? 'active' : ''}`}
            onClick={() => onTabChange('query')}
          >
            <Search size={15} />
            <span>AI Query</span>
          </button>

          <button
            type="button"
            className={`nav-tab ${activeTab === 'sources' ? 'active' : ''}`}
            onClick={() => onTabChange('sources')}
          >
            <GitBranch size={15} />
            <span>Sources & Sync</span>
          </button>

          <button
            type="button"
            className={`nav-tab ${activeTab === 'schema' ? 'active' : ''}`}
            onClick={() => onTabChange('schema')}
          >
            <Database size={15} />
            <span>Schema Explorer</span>
          </button>

          <button
            type="button"
            className={`nav-tab ${activeTab === 'architecture' ? 'active' : ''}`}
            onClick={() => onTabChange('architecture')}
          >
            <Cpu size={15} />
            <span>Architecture</span>
          </button>

          <button
            type="button"
            className={`nav-tab ${activeTab === 'onboarding' ? 'active' : ''}`}
            onClick={() => onTabChange('onboarding')}
          >
            <BookOpen size={15} />
            <span>Onboarding</span>
          </button>
        </nav>

        <div className="header-actions">
          <div className="status-pill">
            <div
              className="status-dot"
              style={{
                backgroundColor: isConnected ? '#10B981' : '#EF4444',
                boxShadow: isConnected ? '0 0 8px #10B981' : '0 0 8px #EF4444',
              }}
            />
            <span>
              {isConnected
                ? `${status?.totalChunksIndexed || 0} Chunks`
                : 'Offline'}
            </span>
          </div>

          <button
            type="button"
            className="btn-secondary"
            onClick={onSync}
            disabled={isSyncing}
          >
            <RefreshCw size={14} className={isSyncing ? 'animate-spin' : ''} />
            <span>{isSyncing ? 'Syncing...' : 'Sync All'}</span>
          </button>

          {user ? (
            <div className="user-profile-badge">
              <span className="user-company">{user.companyName}</span>
              <button
                type="button"
                className="btn-icon text-slate-400 hover:text-slate-600 ml-1"
                onClick={onLogout}
                title="Logout Account"
              >
                <LogOut size={14} />
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="btn-primary-sm"
              onClick={onOpenAuth}
            >
              <User size={13} />
              <span>Account Sign In</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
};
