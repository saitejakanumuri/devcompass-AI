import React, { useState } from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { api } from '../api/client';
import type { QueryResponse } from '../types/api';

export const ChatPage: React.FC = () => {
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<{ role: 'user' | 'ai', text: string, citations?: any[] }[]>([]);

  const handleQuery = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!query.trim()) return;

    const userQuery = query;
    setQuery('');
    setMessages(prev => [...prev, { role: 'user', text: userQuery }]);
    setLoading(true);

    try {
      // provider config is now driven by backend, we just send a default string
      const response: QueryResponse = await api.executeQuery({
        question: userQuery,
        provider: 'gemini', // backend uses activeProvider regardless if not found
        topK: 5
      });
      
      setMessages(prev => [...prev, { 
        role: 'ai', 
        text: response.answer, 
        citations: response.citations 
      }]);
    } catch (err) {
      setMessages(prev => [...prev, { role: 'ai', text: 'Sorry, an error occurred while fetching the answer.' }]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="chat-container">
      <div className="chat-messages">
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '2rem' }}>
            <h2>Welcome to DevCompass AI</h2>
            <p>Ask questions about your codebase, database schemas, or Notion docs.</p>
          </div>
        )}
        
        {messages.map((msg, idx) => (
          <div key={idx} className={`message ${msg.role}`}>
            {msg.role === 'user' ? (
              <p>{msg.text}</p>
            ) : (
              <div>
                <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.text}</ReactMarkdown>
                
                {msg.citations && msg.citations.length > 0 && (
                  <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)' }}>
                    <h4 style={{ fontSize: '0.875rem', marginBottom: '0.5rem' }}>Sources:</h4>
                    <ul style={{ fontSize: '0.875rem', paddingLeft: '1.5rem' }}>
                      {msg.citations.map((c: any, i: number) => (
                        <li key={i}>{c.documentTitle} ({c.sourceType})</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
        {loading && <div className="message ai">Thinking...</div>}
      </div>

      <form className="chat-input-area" onSubmit={handleQuery}>
        <input 
          type="text" 
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Ask a question (e.g. How does authentication work?)" 
          disabled={loading}
        />
        <button type="submit" className="btn" style={{ width: '100px' }} disabled={loading || !query.trim()}>
          Send
        </button>
      </form>
    </div>
  );
};
