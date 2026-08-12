import React, { useState, useEffect } from 'react';
import {
  Copy,
  Check,
  UserCheck
} from 'lucide-react';
import { api } from '../api/client';
import type { OnboardingFlow } from '../types/api';

export const OnboardingTab: React.FC = () => {
  const [flows, setFlows] = useState<OnboardingFlow[]>([]);
  const [loading, setLoading] = useState(true);
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);

  useEffect(() => {
    loadFlows();
  }, []);

  const loadFlows = async () => {
    setLoading(true);
    try {
      const data = await api.getOnboardingFlows();
      setFlows(data || []);
    } catch (err) {
      console.warn('Failed to load onboarding flows:', err);
      setFlows([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCopyCmd = (cmd: string) => {
    navigator.clipboard.writeText(cmd);
    setCopiedCmd(cmd);
    setTimeout(() => setCopiedCmd(null), 2000);
  };

  return (
    <div className="tab-container">
      <div className="tab-header">
        <h2>DevCompass AI Platform & Onboarding Guide</h2>
        <p>Learn about DevCompass AI, the advantages of plugging your Knowledge Sources, and how to explore your system.</p>
      </div>

      {loading ? (
        <div className="p-8 text-center text-slate-500 font-medium">Loading onboarding guides...</div>
      ) : flows.length === 0 ? (
        <div className="p-8 text-center text-slate-500 font-medium">No onboarding flows retrieved from connected backend.</div>
      ) : (
        <div className="onboarding-grid">
          {flows.map((flow) => (
            <div key={flow.id} className="flow-card">
              <div className="flow-header">
                <div>
                  <h3>{flow.title}</h3>
                  <p>{flow.description}</p>
                </div>
                <span className="role-pill">
                  <UserCheck size={12} /> {flow.targetRole || 'Engineer'}
                </span>
              </div>

              <div className="steps-list">
                {flow.steps && flow.steps.map((step: any, idx: number) => {
                  const stepNum = step.stepNumber || step.step || (idx + 1);
                  const stepTitle = step.title || step.name || `Step ${stepNum}`;
                  const stepDesc = step.description || step.detail || '';
                  const stepCmd = step.actionCommand || step.command;

                  return (
                    <div key={stepNum} className="step-item">
                      <div className="step-badge">{stepNum}</div>
                      <div className="step-content">
                        <h4>{stepTitle}</h4>
                        {stepDesc && <p>{stepDesc}</p>}
                        {stepCmd && (
                          <div className="cmd-box">
                            <code>{stepCmd}</code>
                            <button
                              type="button"
                              className="btn-icon"
                              onClick={() => handleCopyCmd(stepCmd)}
                            >
                              {copiedCmd === stepCmd ? (
                                <Check size={14} className="text-emerald-400" />
                              ) : (
                                <Copy size={14} />
                              )}
                            </button>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
