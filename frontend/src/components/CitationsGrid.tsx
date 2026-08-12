import React from 'react';
import { BookOpen, FileText, Database, GitBranch, MessageSquare, CheckSquare } from 'lucide-react';
import type { Citation } from '../types/api';

interface CitationsGridProps {
  citations: Citation[];
}

export const CitationsGrid: React.FC<CitationsGridProps> = ({ citations }) => {
  if (!citations || citations.length === 0) return null;

  const renderIcon = (sourceType: string) => {
    const typeStr = String(sourceType).toUpperCase();
    if (typeStr.includes('NOTION')) {
      return <FileText size={14} className="icon-notion" />;
    } else if (typeStr.includes('DATABASE') || typeStr.includes('SQL')) {
      return <Database size={14} className="icon-db" />;
    } else if (typeStr.includes('GIT') || typeStr.includes('REPOSITORY')) {
      return <GitBranch size={14} className="icon-git" />;
    } else if (typeStr.includes('SLACK')) {
      return <MessageSquare size={14} className="text-pink-500" />;
    } else if (typeStr.includes('JIRA') || typeStr.includes('CONFLUENCE')) {
      return <CheckSquare size={14} className="text-blue-500" />;
    }
    return <BookOpen size={14} />;
  };

  return (
    <div className="citations-section">
      <div className="citations-title">
        <span className="flex items-center gap-1.5">
          <BookOpen size={14} />
          Retrieved Knowledge Sources & Citations
        </span>
        <span className="tag-pill" style={{ fontSize: '0.68rem', opacity: 0.85 }}>
          &ge; 30.0% Match Threshold
        </span>
      </div>

      <div className="citations-grid">
        {citations.map((c) => {
          const excerptText = c.snippet || c.excerpt || '';
          const pathText = c.documentPath || c.sourceUrl || '';

          return (
            <div key={c.id || Math.random().toString()} className="citation-card">
              <div className="citation-header">
                <span className="citation-source">
                  {renderIcon(c.sourceType)}
                  {c.sourceType}
                </span>
                <span className="citation-score">
                  {(c.relevanceScore * 100).toFixed(1)}% Match
                </span>
              </div>

              <div className="citation-name" title={c.sourceTitle}>
                {c.sourceTitle}
              </div>

              {pathText && (
                <div className="text-[0.72rem] font-mono text-slate-400 truncate" title={pathText}>
                  {pathText}
                </div>
              )}

              {excerptText && <p className="citation-excerpt">{excerptText}</p>}
            </div>
          );
        })}
      </div>
    </div>
  );
};
