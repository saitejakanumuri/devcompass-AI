import React from 'react';
import { Cpu, Check } from 'lucide-react';
import type { ProviderConfig } from '../types/api';

interface ProviderSelectorProps {
  providers: ProviderConfig[];
  selectedProvider: string;
  onSelectProvider: (providerKey: string) => void;
}

export const ProviderSelector: React.FC<ProviderSelectorProps> = ({
  providers,
  selectedProvider,
  onSelectProvider,
}) => {
  return (
    <div className="provider-section">
      <div className="section-label">
        <span className="flex items-center gap-1">
          <Cpu size={14} /> Select AI Provider
        </span>
        <span className="active-label">
          Active: {selectedProvider}
        </span>
      </div>

      <div className="provider-pills">
        {providers.map((p) => {
          const providerKey = p.id || p.providerType || p.name;
          const isSelected = selectedProvider.toLowerCase() === providerKey.toLowerCase();

          return (
            <button
              key={providerKey}
              type="button"
              className={`provider-pill ${isSelected ? 'active' : ''}`}
              onClick={() => onSelectProvider(providerKey)}
            >
              <div className={`badge-dot ${isSelected ? 'active-dot' : ''}`} />
              <span>
                {p.name} {p.model ? `(${p.model})` : ''}
              </span>
              {isSelected && <Check size={12} className="check-icon" />}
            </button>
          );
        })}
      </div>
    </div>
  );
};
