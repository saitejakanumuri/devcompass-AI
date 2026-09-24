import React from 'react';
import { api } from '../api/client';
import type { AuthResponse } from '../types/api';

interface NavbarProps {
  user: AuthResponse | null;
  currentPath: string;
  onNavigate: (path: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, currentPath, onNavigate }) => {
  const handleLogout = () => {
    api.logout();
  };

  return (
    <nav className="navbar">
      <a href="#" className="nav-brand" onClick={(e) => { e.preventDefault(); onNavigate('chat'); }}>
        DevCompass AI
      </a>
      
      {user && (
        <div className="nav-links">
          <span 
            className={`nav-link ${currentPath === 'chat' ? 'active' : ''}`}
            onClick={() => onNavigate('chat')}
          >
            Chat
          </span>
          <span 
            className={`nav-link ${currentPath === 'settings' ? 'active' : ''}`}
            onClick={() => onNavigate('settings')}
          >
            Settings
          </span>
          <div style={{ width: '1px', height: '24px', backgroundColor: 'var(--border-color)', margin: '0 0.5rem' }}></div>
          <span style={{ fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            {user.fullName || user.email}
          </span>
          <button className="btn btn-secondary" onClick={handleLogout} style={{ padding: '0.25rem 0.75rem', width: 'auto' }}>
            Logout
          </button>
        </div>
      )}
    </nav>
  );
};
