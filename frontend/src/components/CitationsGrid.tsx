import React from 'react';
import { BookOpen, FileText, Database, GitBranch } from 'lucide-react';
import type { Citation } from '../types/api';

interface CitationsGridProps {
  citations: Citation[];
}

export const CitationsGrid: React.FC<CitationsGridProps> = ({ citations }) => {
  if (!citations || citations.length === 0) return null;

  const renderIcon = (sourceType: string) => {
    switch (sourceType.toUpperCase()) {
      case 'NOTION':
        return <FileText size={14} className="icon-notion" />;
      case 'DATABASE':
      case 'SQL':
        return <Database size={14} className="icon-db" />;
      case 'GIT':
      case 'REPOSITORY':
        return <GitBranch size={14} className="icon-git" />;
      default:
        return <BookOpen size={14} />;
    }
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
        {citations.map((c) => (
          <div key={c.id} className="citation-card">
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

            <p className="citation-excerpt">{c.excerpt}</p>
          </div>
        ))}
      </div>
    </div>
  );
};
