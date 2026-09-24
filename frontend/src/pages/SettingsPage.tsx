import React, { useState, useEffect } from 'react';
import { api } from '../api/client';

export const SettingsPage: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState<string | null>(null);

  // Notion state
  const [notionToken, setNotionToken] = useState('');
  const [notionPageId, setNotionPageId] = useState('');

  // Git state
  const [gitUrl, setGitUrl] = useState('');
  const [gitBranch, setGitBranch] = useState('main');

  // DB state
  const [dbUrl, setDbUrl] = useState('');
  const [dbUser, setDbUser] = useState('');
  const [dbPass, setDbPass] = useState('');

  const fetchConfigs = async () => {
    try {
      const data = await api.getAccountConfigs();
      
      // Populate fields if config exists
      const notion = data.find(c => c.sourceType === 'NOTION');
      if (notion) {
        setNotionToken(notion.configJson.apiToken || notion.configJson.token || '');
        setNotionPageId(notion.configJson.mainPageId || notion.configJson.databaseId || '');
      }

      const git = data.find(c => c.sourceType === 'GIT_REPOSITORY');
      if (git) {
        setGitUrl(git.configJson.repoUrl || '');
        setGitBranch(git.configJson.branch || 'main');
      }

      const db = data.find(c => c.sourceType === 'DATABASE_METADATA');
      if (db) {
        setDbUrl(db.configJson.url || '');
        setDbUser(db.configJson.username || '');
        setDbPass(db.configJson.password || '');
      }
    } catch (err) {
      console.error('Failed to load configs', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleSave = async (sourceType: string, configJson: any) => {
    try {
      setSyncing(sourceType);
      await api.saveAccountConfig(sourceType, configJson, true);
      await fetchConfigs();
      alert('Configuration saved and synced successfully!');
    } catch (err) {
      alert('Failed to save configuration');
    } finally {
      setSyncing(null);
    }
  };

  const handleSync = async (sourceType: string) => {
    try {
      setSyncing(sourceType);
      await api.syncAccountSource(sourceType);
      await fetchConfigs();
      alert('Sync successful!');
    } catch (err) {
      alert('Sync failed');
    } finally {
      setSyncing(null);
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div className="settings-container">
      {/* Notion Config */}
      <div className="config-card">
        <h3>Notion Configuration</h3>
        <div className="form-group">
          <label>API Token</label>
          <input type="password" value={notionToken} onChange={e => setNotionToken(e.target.value)} className="form-input" />
        </div>
        <div className="form-group">
          <label>Main Page / Database ID</label>
          <input type="text" value={notionPageId} onChange={e => setNotionPageId(e.target.value)} className="form-input" />
        </div>
        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
          <button className="btn" onClick={() => handleSave('NOTION', { apiToken: notionToken, mainPageId: notionPageId })} disabled={syncing === 'NOTION'}>
            {syncing === 'NOTION' ? 'Syncing...' : 'Save & Sync'}
          </button>
          <button className="btn btn-secondary" onClick={() => handleSync('NOTION')} disabled={syncing === 'NOTION'}>
            Force Sync
          </button>
        </div>
      </div>

      {/* Git Config */}
      <div className="config-card">
        <h3>Git Repository Configuration</h3>
        <div className="form-group">
          <label>Repository URL</label>
          <input type="text" value={gitUrl} onChange={e => setGitUrl(e.target.value)} className="form-input" placeholder="https://github.com/org/repo.git" />
        </div>
        <div className="form-group">
          <label>Branch</label>
          <input type="text" value={gitBranch} onChange={e => setGitBranch(e.target.value)} className="form-input" />
        </div>
        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
          <button className="btn" onClick={() => handleSave('GIT_REPOSITORY', { repoUrl: gitUrl, branch: gitBranch })} disabled={syncing === 'GIT_REPOSITORY'}>
            {syncing === 'GIT_REPOSITORY' ? 'Syncing...' : 'Save & Sync'}
          </button>
          <button className="btn btn-secondary" onClick={() => handleSync('GIT_REPOSITORY')} disabled={syncing === 'GIT_REPOSITORY'}>
            Force Sync
          </button>
        </div>
      </div>

      {/* DB Config */}
      <div className="config-card">
        <h3>Database Configuration</h3>
        <div className="form-group">
          <label>JDBC URL</label>
          <input type="text" value={dbUrl} onChange={e => setDbUrl(e.target.value)} className="form-input" placeholder="jdbc:postgresql://..." />
        </div>
        <div className="form-group">
          <label>Username</label>
          <input type="text" value={dbUser} onChange={e => setDbUser(e.target.value)} className="form-input" />
        </div>
        <div className="form-group">
          <label>Password</label>
          <input type="password" value={dbPass} onChange={e => setDbPass(e.target.value)} className="form-input" />
        </div>
        <div style={{ display: 'flex', gap: '1rem', marginTop: '1rem' }}>
          <button className="btn" onClick={() => handleSave('DATABASE_METADATA', { url: dbUrl, username: dbUser, password: dbPass })} disabled={syncing === 'DATABASE_METADATA'}>
            {syncing === 'DATABASE_METADATA' ? 'Syncing...' : 'Save & Sync'}
          </button>
          <button className="btn btn-secondary" onClick={() => handleSync('DATABASE_METADATA')} disabled={syncing === 'DATABASE_METADATA'}>
            Force Sync
          </button>
        </div>
      </div>
    </div>
  );
};
