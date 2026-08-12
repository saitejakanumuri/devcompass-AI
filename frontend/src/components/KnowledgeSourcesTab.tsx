import React, { useState, useEffect } from 'react';
import {
  GitBranch,
  Database,
  FileText,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Folder,
  Layers,
  Save,
  Link,
  ShieldCheck,
  Server
} from 'lucide-react';
import { api } from '../api/client';
import type { GitRepoConfig } from '../types/api';

export const KnowledgeSourcesTab: React.FC = () => {
  const [activeSubTab, setActiveSubTab] = useState<'git' | 'database' | 'notion'>('git');

  // Git form state
  const [gitConfig, setGitConfig] = useState<GitRepoConfig>({
    repoUrl: 'https://github.com/org/repo',
    repoPath: './',
    branch: 'main',
    includedExtensions: 'java,ts,tsx,py,go,rs,yml,yaml,md,json',
    maxFileSizeKb: 500,
  });
  const [gitAutoSync, setGitAutoSync] = useState(false);
  const [gitSyncing, setGitSyncing] = useState(false);
  const [gitFeedback, setGitFeedback] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);

  // Database form state
  const [dbUrl, setDbUrl] = useState('jdbc:postgresql://localhost:5432/devcompass');
  const [dbUser, setDbUser] = useState('postgres');
  const [dbPassword, setDbPassword] = useState('postgres');
  const [dbTesting, setDbTesting] = useState(false);
  const [dbSyncing, setDbSyncing] = useState(false);
  const [dbFeedback, setDbFeedback] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);

  // Notion state
  const [notionApiKey, setNotionApiKey] = useState('');
  const [notionMainPageId, setNotionMainPageId] = useState('3b2d87a1-7505-8034-b8e5-e76c150fc6bb');
  const [notionSyncing, setNotionSyncing] = useState(false);
  const [notionFeedback, setNotionFeedback] = useState<{ type: 'success' | 'error'; msg: string } | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const gitRes = await api.getGitConfig();
      if (gitRes) {
        setGitConfig({
          repoUrl: gitRes.repoUrl || 'https://github.com/org/repo',
          repoPath: gitRes.repoPath || './',
          branch: gitRes.branch || 'main',
          includedExtensions: gitRes.includedExtensions || 'java,ts,tsx,py,go,rs,yml,yaml,md,json',
          maxFileSizeKb: gitRes.maxFileSizeKb || 500,
        });
      }

      const configs = await api.getAccountConfigs();
      if (configs && Array.isArray(configs)) {
        const notionCfg = configs.find(c => c.sourceType === 'NOTION');
        if (notionCfg && notionCfg.configJson) {
          if (notionCfg.configJson.apiToken || notionCfg.configJson.apiKey) {
            setNotionApiKey(notionCfg.configJson.apiToken || notionCfg.configJson.apiKey);
          }
          if (notionCfg.configJson.mainPageId || notionCfg.configJson.databaseId) {
            setNotionMainPageId(notionCfg.configJson.mainPageId || notionCfg.configJson.databaseId);
          }
        }

        const dbCfg = configs.find(c => c.sourceType === 'DATABASE_METADATA');
        if (dbCfg && dbCfg.configJson) {
          if (dbCfg.configJson.url || dbCfg.configJson.dbUrl) setDbUrl(dbCfg.configJson.url || dbCfg.configJson.dbUrl);
          if (dbCfg.configJson.username || dbCfg.configJson.dbUser) setDbUser(dbCfg.configJson.username || dbCfg.configJson.dbUser);
          if (dbCfg.configJson.password || dbCfg.configJson.dbPassword) setDbPassword(dbCfg.configJson.password || dbCfg.configJson.dbPassword);
        }
      }
    } catch (err) {
      console.warn('Error loading knowledge sources data:', err);
    }
  };

  const handleSaveGitConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    setGitFeedback(null);
    try {
      await api.updateGitConfig(gitConfig, gitAutoSync);
      setGitFeedback({
        type: 'success',
        msg: gitAutoSync
          ? 'Git configuration updated & background sync executed successfully!'
          : 'Git configuration saved successfully.',
      });
      loadData();
    } catch (err: any) {
      setGitFeedback({
        type: 'error',
        msg: err.response?.data?.message || err.message || 'Failed to save Git repository config.',
      });
    }
  };

  const handleSyncGit = async () => {
    setGitSyncing(true);
    setGitFeedback(null);
    try {
      const res = await api.syncGitRepo();
      setGitFeedback({
        type: 'success',
        msg: `Git repository sync complete! Processed ${res.documentsProcessed} files, generated ${res.totalChunksGenerated} vector chunks.`,
      });
      loadData();
    } catch (err: any) {
      setGitFeedback({
        type: 'error',
        msg: err.response?.data?.message || err.message || 'Git sync failed.',
      });
    } finally {
      setGitSyncing(false);
    }
  };

  const handleTestDb = async () => {
    setDbTesting(true);
    setDbFeedback(null);
    try {
      const res = await api.testDbConnection({ url: dbUrl, username: dbUser, password: dbPassword });
      setDbFeedback({
        type: 'success',
        msg: res.message || 'Successfully established PostgreSQL INFORMATION_SCHEMA connection.',
      });
    } catch (err: any) {
      setDbFeedback({
        type: 'error',
        msg: err.response?.data?.message || err.message || 'Database connection failed.',
      });
    } finally {
      setDbTesting(false);
    }
  };

  const handleSaveAndSyncDb = async () => {
    setDbSyncing(true);
    setDbFeedback(null);
    try {
      await api.saveAccountConfig('DATABASE_METADATA', { url: dbUrl, username: dbUser, password: dbPassword }, true);
      const res = await api.syncDbSchema();
      setDbFeedback({
        type: 'success',
        msg: `Database schema sync complete! Indexed ${res.documentsProcessed} tables and ${res.totalChunksGenerated} schema chunks into pgvector.`,
      });
      loadData();
    } catch (err: any) {
      setDbFeedback({
        type: 'error',
        msg: err.response?.data?.message || err.message || 'Database schema sync failed.',
      });
    } finally {
      setDbSyncing(false);
    }
  };

  const handleSaveAndSyncNotion = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setNotionSyncing(true);
    setNotionFeedback(null);
    try {
      await api.saveAccountConfig('NOTION', {
        apiToken: notionApiKey,
        mainPageId: notionMainPageId,
      }, true);
      const res = await api.syncAccountSource('NOTION');
      setNotionFeedback({
        type: 'success',
        msg: `Notion workspace sync complete! Ingested ${res.documentsProcessed || 0} pages and updated vector store.`,
      });
      loadData();
    } catch (err: any) {
      setNotionFeedback({
        type: 'error',
        msg: err.response?.data?.message || err.message || 'Notion sync failed. Verify your API Key and Page ID.',
      });
    } finally {
      setNotionSyncing(false);
    }
  };

  return (
    <div className="tab-container">
      <div className="tab-header">
        <h2>Knowledge Sources & Pipeline Connectors</h2>
        <p>Configure internal source repositories, database connections, and Notion workspaces for automated RAG indexing.</p>
      </div>

      {/* Sources Overview Grid */}
      <div className="sources-status-grid">
        <div className="status-card">
          <div className="card-top">
            <div className="icon-wrapper git">
              <GitBranch size={20} />
            </div>
            <span className="source-title">Git Repository</span>
            <span className="badge-healthy">Configured</span>
          </div>
          <p className="card-desc">Indexes backend source files, architecture docs, and CI/CD pipelines.</p>
          <div className="card-footer">
            <button
              type="button"
              className="btn-sm btn-secondary"
              onClick={() => setActiveSubTab('git')}
            >
              Configure
            </button>
            <button
              type="button"
              className="btn-sm btn-primary"
              onClick={handleSyncGit}
              disabled={gitSyncing}
            >
              <RefreshCw size={12} className={gitSyncing ? 'animate-spin' : ''} />
              <span>{gitSyncing ? 'Syncing...' : 'Sync Git'}</span>
            </button>
          </div>
        </div>

        <div className="status-card">
          <div className="card-top">
            <div className="icon-wrapper db">
              <Database size={20} />
            </div>
            <span className="source-title">Database Metadata</span>
            <span className="badge-healthy">INFORMATION_SCHEMA</span>
          </div>
          <p className="card-desc">Indexes relational schemas, table metadata, primary/foreign keys, and vector indices.</p>
          <div className="card-footer">
            <button
              type="button"
              className="btn-sm btn-secondary"
              onClick={() => setActiveSubTab('database')}
            >
              Configure
            </button>
            <button
              type="button"
              className="btn-sm btn-primary"
              onClick={handleSaveAndSyncDb}
              disabled={dbSyncing}
            >
              <RefreshCw size={12} className={dbSyncing ? 'animate-spin' : ''} />
              <span>{dbSyncing ? 'Syncing...' : 'Sync DB'}</span>
            </button>
          </div>
        </div>

        <div className="status-card">
          <div className="card-top">
            <div className="icon-wrapper notion">
              <FileText size={20} />
            </div>
            <span className="source-title">Notion Workspace</span>
            <span className="badge-healthy">Webhook Active</span>
          </div>
          <p className="card-desc">Syncs tech specs, API contracts, and engineering onboarding documentation.</p>
          <div className="card-footer">
            <button
              type="button"
              className="btn-sm btn-secondary"
              onClick={() => setActiveSubTab('notion')}
            >
              View Webhook
            </button>
            <button
              type="button"
              className="btn-sm btn-primary"
              onClick={() => handleSaveAndSyncNotion()}
              disabled={notionSyncing}
            >
              <RefreshCw size={12} className={notionSyncing ? 'animate-spin' : ''} />
              <span>{notionSyncing ? 'Syncing...' : 'Sync Notion'}</span>
            </button>
          </div>
        </div>
      </div>

      {/* Sub-Tab Control */}
      <div className="subtab-bar">
        <button
          type="button"
          className={`subtab-btn ${activeSubTab === 'git' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('git')}
        >
          <GitBranch size={16} /> Git Repository Config
        </button>
        <button
          type="button"
          className={`subtab-btn ${activeSubTab === 'database' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('database')}
        >
          <Database size={16} /> Database Connection
        </button>
        <button
          type="button"
          className={`subtab-btn ${activeSubTab === 'notion' ? 'active' : ''}`}
          onClick={() => setActiveSubTab('notion')}
        >
          <FileText size={16} /> Notion Webhook
        </button>
      </div>

      {/* Git Config Form */}
      {activeSubTab === 'git' && (
        <form className="config-form-card" onSubmit={handleSaveGitConfig}>
          <div className="form-header">
            <h3>
              <GitBranch size={18} className="text-indigo-600" />
              Git Repository Ingestion Settings
            </h3>
            <p>Define local workspace paths or remote repository URLs for incremental chunking.</p>
          </div>

          {gitFeedback && (
            <div className={`feedback-alert ${gitFeedback.type}`}>
              {gitFeedback.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <span>{gitFeedback.msg}</span>
            </div>
          )}

          <div className="form-grid">
            <div className="input-group full-width">
              <label htmlFor="repoUrl">
                <Link size={14} /> Repository URL
              </label>
              <input
                id="repoUrl"
                type="text"
                value={gitConfig.repoUrl}
                onChange={(e) => setGitConfig({ ...gitConfig, repoUrl: e.target.value })}
                placeholder="https://github.com/your-org/your-repo"
              />
            </div>

            <div className="input-group">
              <label htmlFor="repoPath">
                <Folder size={14} /> Workspace Path
              </label>
              <input
                id="repoPath"
                type="text"
                value={gitConfig.repoPath}
                onChange={(e) => setGitConfig({ ...gitConfig, repoPath: e.target.value })}
                placeholder="./"
              />
            </div>

            <div className="input-group">
              <label htmlFor="branch">
                <GitBranch size={14} /> Default Branch
              </label>
              <input
                id="branch"
                type="text"
                value={gitConfig.branch}
                onChange={(e) => setGitConfig({ ...gitConfig, branch: e.target.value })}
                placeholder="main"
              />
            </div>

            <div className="input-group full-width">
              <label htmlFor="includedExtensions">
                <Layers size={14} /> Included File Extensions (comma separated)
              </label>
              <input
                id="includedExtensions"
                type="text"
                value={gitConfig.includedExtensions}
                onChange={(e) => setGitConfig({ ...gitConfig, includedExtensions: e.target.value })}
                placeholder="java,ts,tsx,py,go,rs,yml,yaml,md,json"
              />
            </div>

            <div className="input-group">
              <label htmlFor="maxFileSizeKb">Max File Size (KB)</label>
              <input
                id="maxFileSizeKb"
                type="number"
                value={gitConfig.maxFileSizeKb}
                onChange={(e) =>
                  setGitConfig({ ...gitConfig, maxFileSizeKb: parseInt(e.target.value) || 500 })
                }
              />
            </div>

            <div className="checkbox-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={gitAutoSync}
                  onChange={(e) => setGitAutoSync(e.target.checked)}
                />
                <span>Automatically trigger ingestion sync after saving</span>
              </label>
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" className="btn-primary">
              <Save size={16} /> Save Configuration
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={handleSyncGit}
              disabled={gitSyncing}
            >
              <RefreshCw size={16} className={gitSyncing ? 'animate-spin' : ''} />
              <span>{gitSyncing ? 'Ingesting Git...' : 'Trigger Git Sync Now'}</span>
            </button>
          </div>
        </form>
      )}

      {/* Database Config Form */}
      {activeSubTab === 'database' && (
        <div className="config-form-card">
          <div className="form-header">
            <h3>
              <Database size={18} className="text-emerald-600" />
              PostgreSQL Relational Schema Connection
            </h3>
            <p>Connect to target PostgreSQL database to index tables, columns, foreign keys, and vector indices.</p>
          </div>

          {dbFeedback && (
            <div className={`feedback-alert ${dbFeedback.type}`}>
              {dbFeedback.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <span>{dbFeedback.msg}</span>
            </div>
          )}

          <div className="form-grid">
            <div className="input-group full-width">
              <label htmlFor="dbUrl">
                <Server size={14} /> Database JDBC Connection URL
              </label>
              <input
                id="dbUrl"
                type="text"
                value={dbUrl}
                onChange={(e) => setDbUrl(e.target.value)}
                placeholder="jdbc:postgresql://localhost:5432/devcompass"
              />
            </div>

            <div className="input-group">
              <label htmlFor="dbUser">Database Username</label>
              <input
                id="dbUser"
                type="text"
                value={dbUser}
                onChange={(e) => setDbUser(e.target.value)}
                placeholder="postgres"
              />
            </div>

            <div className="input-group">
              <label htmlFor="dbPassword">Database Password</label>
              <input
                id="dbPassword"
                type="password"
                value={dbPassword}
                onChange={(e) => setDbPassword(e.target.value)}
                placeholder="••••••••"
              />
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="btn-secondary"
              onClick={handleTestDb}
              disabled={dbTesting}
            >
              <ShieldCheck size={16} />
              <span>{dbTesting ? 'Testing Connection...' : 'Test Connection'}</span>
            </button>

            <button
              type="button"
              className="btn-primary"
              onClick={handleSaveAndSyncDb}
              disabled={dbSyncing}
            >
              <RefreshCw size={16} className={dbSyncing ? 'animate-spin' : ''} />
              <span>{dbSyncing ? 'Syncing Schema...' : 'Save & Sync Schema'}</span>
            </button>
          </div>
        </div>
      )}

      {/* Notion Form / Info */}
      {activeSubTab === 'notion' && (
        <div className="config-form-card">
          <div className="form-header">
            <h3>
              <FileText size={18} className="text-blue-600" />
              Notion Integration Credentials & Webhook Connector
            </h3>
            <p>Provide your Notion Integration Secret Key (API Key) and Main Page ID / Root Database ID to extract documentation and sync updates.</p>
          </div>

          {notionFeedback && (
            <div className={`feedback-alert ${notionFeedback.type}`}>
              {notionFeedback.type === 'success' ? <CheckCircle2 size={16} /> : <AlertCircle size={16} />}
              <span>{notionFeedback.msg}</span>
            </div>
          )}

          <div className="form-grid">
            <div className="input-group full-width">
              <label htmlFor="notionApiKey">
                <Link size={14} /> Notion Integration API Secret Key (API Key)
              </label>
              <input
                id="notionApiKey"
                type="password"
                value={notionApiKey}
                onChange={(e) => setNotionApiKey(e.target.value)}
                placeholder="ntn_... or secret_..."
              />
            </div>

            <div className="input-group full-width">
              <label htmlFor="notionMainPageId">
                <Folder size={14} /> Notion Main Page ID / Database ID
              </label>
              <input
                id="notionMainPageId"
                type="text"
                value={notionMainPageId}
                onChange={(e) => setNotionMainPageId(e.target.value)}
                placeholder="3b2d87a1-7505-8034-b8e5-e76c150fc6bb"
              />
            </div>
          </div>

          <div className="info-box">
            <div className="info-row">
              <strong>Webhook Receiver URL:</strong>
              <code>http://localhost:8081/api/v1/notion/webhook</code>
            </div>
            <div className="info-row">
              <strong>Supported Events:</strong>
              <span className="badge-pill">page_created</span>
              <span className="badge-pill">page_updated</span>
              <span className="badge-pill">database_entry_changed</span>
            </div>
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="btn-primary"
              onClick={handleSaveAndSyncNotion}
              disabled={notionSyncing}
            >
              <RefreshCw size={16} className={notionSyncing ? 'animate-spin' : ''} />
              <span>{notionSyncing ? 'Syncing Notion...' : 'Save & Sync Notion'}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
