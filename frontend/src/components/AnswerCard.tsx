import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Copy, Check, Clock, Zap, Cpu } from 'lucide-react';
import type { QueryResponse } from '../types/api';
import { CitationsGrid } from './CitationsGrid';

interface AnswerCardProps {
  response: QueryResponse | null;
  isLoading: boolean;
}

export const AnswerCard: React.FC<AnswerCardProps> = ({
  response,
  isLoading,
}) => {
  const [copied, setCopied] = useState(false);

  if (isLoading) {
    return (
      <div className="loading-state">
        <div className="skeleton-line" style={{ width: '40%' }} />
        <div className="skeleton-line" style={{ width: '90%' }} />
        <div className="skeleton-line" style={{ width: '75%' }} />
        <div className="skeleton-line" style={{ width: '60%' }} />
      </div>
    );
  }

  if (!response) return null;

  const handleCopy = () => {
    if (!response.answer) return;
    navigator.clipboard.writeText(response.answer);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="response-card">
      <div className="response-header">
        <div className="response-meta">
          <span className="tag-pill provider">
            <Cpu size={12} /> {response.providerUsed}
          </span>
          <span className="tag-pill">
            <Clock size={12} /> {response.latencyMs} ms
          </span>
          <span className="tag-pill">
            <Zap size={12} /> {response.promptTokens + response.completionTokens} tokens
          </span>
        </div>

        <button type="button" className="btn-icon" onClick={handleCopy} title="Copy Answer">
          {copied ? <Check size={16} className="text-emerald-400" /> : <Copy size={16} />}
        </button>
      </div>

      <div className="response-body">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>
          {response.answer}
        </ReactMarkdown>
      </div>

      <CitationsGrid citations={response.citations} />
    </div>
  );
};
