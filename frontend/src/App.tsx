import React, { useState, useEffect } from 'react';
import { api } from './api/client';
import { Navbar } from './components/Navbar';
import { AuthPage } from './pages/AuthPage';
import { ChatPage } from './pages/ChatPage';
import { SettingsPage } from './pages/SettingsPage';
import type { AuthResponse } from './types/api';
import './index.css';

export const App: React.FC = () => {
  const [user, setUser] = useState<AuthResponse | null>(null);
  const [currentPath, setCurrentPath] = useState<string>('chat');

  useEffect(() => {
    const storedUser = api.getStoredUser();
    if (storedUser) {
      setUser(storedUser);
    }
  }, []);

  const handleLoginSuccess = (loggedInUser: AuthResponse) => {
    setUser(loggedInUser);
    setCurrentPath('chat');
  };

  const renderContent = () => {
    if (!user) {
      return <AuthPage onLoginSuccess={handleLoginSuccess} />;
    }

    if (currentPath === 'settings') {
      return <SettingsPage />;
    }

    return <ChatPage />;
  };

  return (
    <div className="app-container">
      <Navbar user={user} currentPath={currentPath} onNavigate={setCurrentPath} />
      <main className="main-content">
        {renderContent()}
      </main>
    </div>
  );
};

export default App;
