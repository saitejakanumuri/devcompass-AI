import React from 'react';
import {
  MessageSquarePlus,
  Compass,
  Search,
  Database,
  GitBranch,
  Cpu,
  BookOpen,
  User,
  LogOut,
  Sparkles,
  Zap,
} from 'lucide-react';
import type { PipelineStatus, AuthResponse } from '../types/api';
import type { TabType } from './Header';

interface SidebarProps {
  activeTab: TabType;
  onTabChange: (tab: TabType) => void;
  currentUser: AuthResponse | null;
  status: PipelineStatus | null;
  onOpenAuth: () => void;
  onLogout: () => void;
  onNewChat?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  activeTab,
  onTabChange,
  currentUser,
  status,
  onOpenAuth,
  onLogout,
  onNewChat,
}) => {
  const isConnected = status?.rdsInitialized ?? false;

  const navItems = [
    { id: 'query' as TabType, label: 'AI Chat & Query', icon: Search, badge: 'RAG' },
    { id: 'sources' as TabType, label: 'Knowledge Sources', icon: GitBranch, badge: 'Plug' },
    { id: 'schema' as TabType, label: 'Schema Explorer', icon: Database, badge: 'Live' },
    { id: 'architecture' as TabType, label: 'Architecture Topo', icon: Cpu },
    { id: 'onboarding' as TabType, label: 'Platform Guide', icon: BookOpen },
  ];

  const userInitial = currentUser?.fullName
    ? currentUser.fullName.charAt(0).toUpperCase()
    : currentUser?.email
    ? currentUser.email.charAt(0).toUpperCase()
    : 'U';

  return (
    <aside className="app-sidebar">
      {/* Brand */}
      <div className="sidebar-brand">
        <div className="sidebar-logo-row">
          <div className="sidebar-logo-icon">
            <Compass size={20} />
          </div>
          <div>
            <div className="sidebar-app-name">
              DevCompass <span className="sidebar-ai-badge">AI</span>
            </div>
            <div className="sidebar-tagline">Architecture Intelligence</div>
          </div>
        </div>
      </div>

      {/* New Query Button */}
      <div className="sidebar-new-btn-wrap">
        <button
          type="button"
          className="sidebar-new-btn"
          onClick={() => {
            onTabChange('query');
            if (onNewChat) onNewChat();
          }}
        >
          <MessageSquarePlus size={15} />
          <span>New AI Query</span>
          <Sparkles size={13} style={{ marginLeft: 'auto', opacity: 0.7 }} />
        </button>
      </div>

      {/* Navigation */}
      <nav className="sidebar-nav">
        <div className="sidebar-section-label">Workspace</div>
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              type="button"
              className={`sidebar-nav-item${isActive ? ' active' : ''}`}
              onClick={() => onTabChange(item.id)}
            >
              <Icon size={16} />
              <span className="sidebar-nav-label">{item.label}</span>
              {item.badge && (
                <span className="sidebar-nav-badge">{item.badge}</span>
              )}
            </button>
          );
        })}
      </nav>

      {/* Status */}
      <div className="sidebar-status">
        <div className={`sidebar-status-dot${isConnected ? ' connected' : ''}`} />
        <span className="sidebar-status-label">
          {isConnected ? 'System Live' : 'Offline'}
        </span>
        <Zap size={12} style={{ marginLeft: 'auto', opacity: 0.6 }} />
      </div>

      {/* Footer / User */}
      <div className="sidebar-footer">
        {currentUser ? (
          <div className="sidebar-user-card">
            <div className="sidebar-avatar">{userInitial}</div>
            <div className="sidebar-user-info">
              <div className="sidebar-user-name">
                {currentUser.fullName || currentUser.email}
              </div>
              <div className="sidebar-user-account">Account</div>
            </div>
            <button
              type="button"
              title="Sign Out"
              className="sidebar-logout-btn"
              onClick={onLogout}
            >
              <LogOut size={14} />
            </button>
          </div>
        ) : (
          <button
            type="button"
            className="sidebar-signin-btn"
            onClick={onOpenAuth}
          >
            <User size={14} />
            <span>Sign In / Create Account</span>
          </button>
        )}
      </div>
    </aside>
  );
};
