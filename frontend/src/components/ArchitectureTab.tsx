import React, { useState, useEffect } from 'react';
import {
  Cpu,
  Database,
  Zap,
  CheckCircle,
  Server,
  Layers,
  Code
} from 'lucide-react';
import { api } from '../api/client';
import type { ArchitectureDetails, PipelineStatus } from '../types/api';

export const ArchitectureTab: React.FC = () => {
  const [arch, setArch] = useState<ArchitectureDetails | null>(null);
  const [status, setStatus] = useState<PipelineStatus | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [archRes, statusRes] = await Promise.all([
        api.getArchitecture(),
        api.getPipelineStatus(),
      ]);
      setArch(archRes);
      setStatus(statusRes);
    } catch (err) {
      console.warn('Failed to load architecture data:', err);
    }
  };

  return (
    <div className="tab-container">
      <div className="tab-header">
        <h2>Production Architecture & Vector Telemetry</h2>
        <p>Live technical stack components powering RAG knowledge retrieval and LLM context synthesis.</p>
      </div>

      {/* Telemetry Stat Cards */}
      <div className="telemetry-grid">
        <div className="telemetry-card">
          <div className="telemetry-icon purple">
            <Layers size={20} />
          </div>
          <div>
            <div className="telemetry-value">{status?.totalChunksIndexed || 0}</div>
            <div className="telemetry-label">Indexed Vector Chunks</div>
          </div>
        </div>

        <div className="telemetry-card">
          <div className="telemetry-icon blue">
            <Cpu size={20} />
          </div>
          <div>
            <div className="telemetry-value">
              {arch?.embeddingsEngine?.model || 'nomic-embed-text'}
            </div>
            <div className="telemetry-label">Embeddings Model ({status?.dimension || 768} dim)</div>
          </div>
        </div>

        <div className="telemetry-card">
          <div className="telemetry-icon emerald">
            <Database size={20} />
          </div>
          <div>
            <div className="telemetry-value">
              {arch?.vectorDatabase?.database || 'PostgreSQL pgvector'}
            </div>
            <div className="telemetry-label">
              {arch?.vectorDatabase?.distanceMetric || 'Cosine Distance (<=>)'}
            </div>
          </div>
        </div>

        <div className="telemetry-card">
          <div className="telemetry-icon amber">
            <Zap size={20} />
          </div>
          <div>
            <div className="telemetry-value">
              {arch?.llmAnswerGenerator?.provider || 'Google Gemini'}
            </div>
            <div className="telemetry-label">1M Context Window LLM</div>
          </div>
        </div>
      </div>

      {/* Main Architecture Diagram / Cards */}
      <div className="arch-details-grid">
        {/* Embeddings Engine Card */}
        <div className="arch-card">
          <div className="arch-card-header">
            <Server size={18} className="text-indigo-600" />
            <h3>Vector Embeddings Engine</h3>
            <span className="badge-healthy ml-auto">
              <CheckCircle size={12} /> {arch?.embeddingsEngine?.status || 'ONLINE'}
            </span>
          </div>

          <div className="arch-card-body">
            <div className="arch-prop">
              <span>Provider:</span>
              <strong>{arch?.embeddingsEngine?.provider || 'Ollama'}</strong>
            </div>
            <div className="arch-prop">
              <span>Model Name:</span>
              <code>{arch?.embeddingsEngine?.model || 'nomic-embed-text'}</code>
            </div>
            <div className="arch-prop">
              <span>Local Endpoint:</span>
              <code>{arch?.embeddingsEngine?.endpoint || 'http://localhost:11434/api/embeddings'}</code>
            </div>
            <div className="arch-prop">
              <span>Dimension Vector:</span>
              <strong>{arch?.embeddingsEngine?.dimension || 768} Float32 Dimensions</strong>
            </div>
          </div>
        </div>

        {/* Vector DB Card */}
        <div className="arch-card">
          <div className="arch-card-header">
            <Database size={18} className="text-emerald-600" />
            <h3>PostgreSQL + pgvector Store</h3>
            <span className="badge-healthy ml-auto">
              <CheckCircle size={12} /> Connected
            </span>
          </div>

          <div className="arch-card-body">
            <div className="arch-prop">
              <span>Database Engine:</span>
              <strong>{arch?.vectorDatabase?.database || 'pgvector'}</strong>
            </div>
            <div className="arch-prop">
              <span>Table Target:</span>
              <code>{arch?.vectorDatabase?.tableName || 'document_chunks'}</code>
            </div>
            <div className="arch-prop">
              <span>Similarity Metric:</span>
              <strong className="text-emerald-600">{arch?.vectorDatabase?.distanceMetric || 'Cosine Distance (<=>)'}</strong>
            </div>
            <div className="arch-prop">
              <span>RDS Status:</span>
              <strong>Initialized & Indexed</strong>
            </div>
          </div>
        </div>

        {/* LLM Generator Card */}
        <div className="arch-card">
          <div className="arch-card-header">
            <Zap size={18} className="text-amber-500" />
            <h3>AI Answer Generation Engine</h3>
            <span className="badge-healthy ml-auto">
              <CheckCircle size={12} /> {arch?.llmAnswerGenerator?.status || 'READY'}
            </span>
          </div>

          <div className="arch-card-body">
            <div className="arch-prop">
              <span>Primary Provider:</span>
              <strong>{arch?.llmAnswerGenerator?.provider || 'Google Gemini'}</strong>
            </div>
            <div className="arch-prop">
              <span>Active Model:</span>
              <code>{arch?.llmAnswerGenerator?.model || 'gemini-1.5-pro'}</code>
            </div>
            <div className="arch-prop">
              <span>Context Capacity:</span>
              <strong>{arch?.llmAnswerGenerator?.contextWindow || '1,000,000 tokens'}</strong>
            </div>
            <div className="arch-prop">
              <span>Dynamic Providers:</span>
              <span>OpenAI, Anthropic Claude, Ollama Supported</span>
            </div>
          </div>
        </div>
      </div>

      {/* Vector Cosine Distance Sample SQL */}
      <div className="sql-box-card">
        <div className="sql-box-header">
          <Code size={16} className="text-indigo-400" />
          <span>pgvector Cosine Distance Retrieval Query</span>
        </div>
        <pre className="sql-code">
          <code>
            {arch?.vectorDatabase?.sampleQuery ||
              `SELECT id, document_title, source_type, content, 
       1 - (embedding <=> '[0.012,-0.045,...]'::vector) AS cosine_similarity
FROM document_chunks
WHERE 1 - (embedding <=> '[0.012,-0.045,...]'::vector) >= 0.70
ORDER BY embedding <=> '[0.012,-0.045,...]'::vector ASC
LIMIT 5;`}
          </code>
        </pre>
      </div>
    </div>
  );
};
