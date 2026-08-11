import React from 'react';
import { Compass, RefreshCw } from 'lucide-react';
import type { PipelineStatus } from '../types/api';

interface HeaderProps {
  status: PipelineStatus | null;
  isConnected: boolean;
  isSyncing: boolean;
  onSync: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  status,
  isConnected,
  isSyncing,
  onSync,
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
                ? `${status?.totalChunksIndexed || 0} Chunks Indexed (${status?.activeVectorDatabase || 'pgvector'})`
                : 'Backend Offline'}
            </span>
          </div>

          <button
            type="button"
            className="btn-secondary"
            onClick={onSync}
            disabled={isSyncing}
          >
            <RefreshCw size={14} className={isSyncing ? 'animate-spin' : ''} />
            <span>{isSyncing ? 'Syncing...' : 'Sync Knowledge'}</span>
          </button>
        </div>
      </div>
    </header>
  );
};
