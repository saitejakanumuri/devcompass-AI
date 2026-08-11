import React, { useState, type KeyboardEvent } from 'react';
import { Send, Sliders } from 'lucide-react';

interface QueryCardProps {
  onExecuteQuery: (question: string, topK: number, temperature: number) => void;
  isLoading: boolean;
}

const SAMPLE_PROMPTS = [
  '💡 Explain our system architecture and data flows',
  '🗄️ What database tables are indexed in PostgreSQL?',
  '🔄 How does the Notion knowledge sync pipeline work?',
  '🚀 Where is the CI/CD deployment flow configured?',
];

export const QueryCard: React.FC<QueryCardProps> = ({
  onExecuteQuery,
  isLoading,
}) => {
  const [question, setQuestion] = useState('');
  const [topK, setTopK] = useState(5);
  const [temperature, setTemperature] = useState(0.2);

  const handleSubmit = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!question.trim() || isLoading) return;
    onExecuteQuery(question.trim(), topK, temperature);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault();
      handleSubmit();
    }
  };

  const handleSampleClick = (promptText: string) => {
    const cleanText = promptText.replace(/^[^\s]+\s*/, '');
    setQuestion(cleanText);
    onExecuteQuery(cleanText, topK, temperature);
  };

  return (
    <div className="query-section">
      <div className="query-card">
        <textarea
          className="query-textarea"
          placeholder="Ask about system architecture, database relationships, or CI/CD pipelines..."
          rows={3}
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          onKeyDown={handleKeyDown}
        />

        <div className="query-toolbar">
          <div className="toolbar-options">
            <div className="option-group">
              <Sliders size={14} />
              <label htmlFor="topK">Top-K:</label>
              <input
                type="range"
                id="topK"
                min="1"
                max="10"
                value={topK}
                onChange={(e) => setTopK(parseInt(e.target.value))}
              />
              <span className="val-badge">{topK}</span>
            </div>

            <div className="option-group">
              <label htmlFor="temp">Temp:</label>
              <input
                type="range"
                id="temp"
                min="0"
                max="1"
                step="0.1"
                value={temperature}
                onChange={(e) => setTemperature(parseFloat(e.target.value))}
              />
              <span className="val-badge">{temperature}</span>
            </div>
          </div>

          <button
            type="button"
            className="btn-submit"
            onClick={handleSubmit}
            disabled={isLoading || !question.trim()}
          >
            <span>Query System</span>
            <Send size={14} />
          </button>
        </div>
      </div>

      <div className="sample-prompts">
        {SAMPLE_PROMPTS.map((prompt, idx) => (
          <button
            key={idx}
            type="button"
            className="prompt-chip"
            onClick={() => handleSampleClick(prompt)}
          >
            {prompt}
          </button>
        ))}
      </div>
    </div>
  );
};
