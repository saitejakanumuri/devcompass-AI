import React, { useState, useEffect } from 'react';
import {
  Database,
  Table as TableIcon,
  Key,
  Link2,
  RefreshCw,
  Search,
  CheckCircle,
  Cpu,
  Layers
} from 'lucide-react';
import { api } from '../api/client';
import type { SchemaMetadata } from '../types/api';

export const SchemaExplorerTab: React.FC = () => {
  const [tables, setTables] = useState<SchemaMetadata[]>([]);
  const [selectedTable, setSelectedTable] = useState<SchemaMetadata | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    loadTables();
  }, []);

  const loadTables = async () => {
    setLoading(true);
    try {
      const data = await api.getTables();
      setTables(data);
      if (data.length > 0) {
        setSelectedTable(data[0]);
      }
    } catch (err) {
      console.warn('Failed to load table schemas:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSyncSchema = async () => {
    setSyncing(true);
    try {
      await api.syncDbSchema();
      await loadTables();
    } catch (err: any) {
      alert('Failed to sync database schema: ' + (err.message || err));
    } finally {
      setSyncing(false);
    }
  };

  const filteredTables = tables.filter((t) =>
    t.tableName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    t.tableSchema.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const currentUser = api.getStoredUser();

  return (
    <div className="tab-container">
      <div className="tab-header flex-between">
        <div>
          <div className="flex items-center gap-2">
            <Database size={24} className="text-indigo-600" />
            <h2>Account-Level PostgreSQL Schema Explorer</h2>
          </div>
          <p>
            Inspect target database tables, relational schemas, foreign keys, and vector column definitions for{' '}
            <span className="font-semibold text-indigo-700">{currentUser?.companyName || 'Account'}</span>.
          </p>
        </div>
        <button
          type="button"
          className="btn-secondary"
          onClick={handleSyncSchema}
          disabled={syncing}
        >
          <RefreshCw size={14} className={syncing ? 'animate-spin' : ''} />
          <span>{syncing ? 'Re-indexing Schema...' : 'Re-index Schema'}</span>
        </button>
      </div>

      <div className="schema-layout">
        {/* Table Sidebar */}
        <div className="schema-sidebar">
          <div className="search-box">
            <Search size={14} className="search-icon" />
            <input
              type="text"
              placeholder="Search tables..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          <div className="table-list">
            {loading ? (
              <div className="p-4 text-center text-sm text-slate-400">Loading tables...</div>
            ) : filteredTables.length === 0 ? (
              <div className="p-4 text-center text-sm text-slate-400">No tables found</div>
            ) : (
              filteredTables.map((tbl) => {
                const isSelected = selectedTable?.tableName === tbl.tableName;
                return (
                  <button
                    key={`${tbl.tableSchema}.${tbl.tableName}`}
                    type="button"
                    className={`table-item ${isSelected ? 'active' : ''}`}
                    onClick={() => setSelectedTable(tbl)}
                  >
                    <div className="table-item-header">
                      <TableIcon size={14} className="text-indigo-500" />
                      <span className="table-name">{tbl.tableName}</span>
                    </div>
                    <div className="table-item-meta">
                      <span className="schema-tag">{tbl.tableSchema}</span>
                      {tbl.vectorIndexed && (
                        <span className="vector-badge">
                          <Cpu size={10} /> pgvector
                        </span>
                      )}
                    </div>
                  </button>
                );
              })
            )}
          </div>
        </div>

        {/* Selected Table Detail Content */}
        <div className="schema-detail">
          {selectedTable ? (
            <div className="detail-card">
              <div className="detail-header">
                <div>
                  <div className="flex items-center gap-2">
                    <Database size={20} className="text-indigo-600" />
                    <h3 className="text-xl font-bold text-slate-800">
                      {selectedTable.tableSchema}.{selectedTable.tableName}
                    </h3>
                  </div>
                  {selectedTable.description && (
                    <p className="detail-desc">{selectedTable.description}</p>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <span className="tag-pill">
                    <Layers size={12} /> {selectedTable.columns.length} Columns
                  </span>
                  {selectedTable.vectorIndexed ? (
                    <span className="badge-healthy">
                      <CheckCircle size={12} /> Vector Indexed
                    </span>
                  ) : (
                    <span className="badge-pill">Unindexed</span>
                  )}
                </div>
              </div>

              {/* Primary Keys */}
              {selectedTable.primaryKeys && selectedTable.primaryKeys.length > 0 && (
                <div className="keys-box">
                  <div className="box-label">
                    <Key size={14} className="text-amber-500" /> Primary Key Constraints
                  </div>
                  <div className="key-chips">
                    {selectedTable.primaryKeys.map((pk) => (
                      <span key={pk} className="pk-chip">
                        {pk}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Columns Table */}
              <div className="columns-section">
                <h4>Column Definitions</h4>
                <div className="table-wrapper">
                  <table className="schema-table">
                    <thead>
                      <tr>
                        <th>Column Name</th>
                        <th>Data Type</th>
                        <th>Nullable</th>
                        <th>Key / Description</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedTable.columns.map((col) => {
                        const isPk = selectedTable.primaryKeys?.includes(col.name);
                        return (
                          <tr key={col.name}>
                            <td className="font-mono font-medium text-slate-800">
                              <span className="flex items-center gap-1.5">
                                {isPk && <Key size={12} className="text-amber-500" />}
                                {col.name}
                              </span>
                            </td>
                            <td className="font-mono text-indigo-600 text-xs">{col.dataType}</td>
                            <td>
                              <span className={`null-pill ${col.nullable ? 'nullable' : 'not-null'}`}>
                                {col.nullable ? 'YES' : 'NO'}
                              </span>
                            </td>
                            <td className="text-slate-500 text-xs">
                              {col.description || (isPk ? 'Primary Key Column' : '-')}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Foreign Keys */}
              {selectedTable.foreignKeys && selectedTable.foreignKeys.length > 0 && (
                <div className="fk-section">
                  <h4>
                    <Link2 size={14} className="text-blue-500" /> Foreign Key Relationships
                  </h4>
                  <div className="fk-grid">
                    {selectedTable.foreignKeys.map((fk, idx) => (
                      <div key={idx} className="fk-card">
                        <span className="fk-source">{fk.columnName}</span>
                        <span className="fk-arrow">&rarr;</span>
                        <span className="fk-target">
                          {fk.targetTable}.{fk.targetColumn}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="empty-detail">Select a table from the sidebar to inspect its relational schema.</div>
          )}
        </div>
      </div>
    </div>
  );
};
