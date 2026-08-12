import React, { useState } from 'react';
import { X, Lock, Mail, User, LogIn, UserPlus, ArrowRight, AlertTriangle } from 'lucide-react';
import { api } from '../api/client';
import type { AuthResponse } from '../types/api';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (user: AuthResponse) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg(null);

    try {
      if (mode === 'login') {
        const res = await api.login({ email, password });
        onSuccess(res);
        onClose();
      } else {
        const res = await api.register({
          email,
          password,
          fullName,
        });
        onSuccess(res);
        onClose();
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Authentication failed.';
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <div className="modal-card max-w-sm w-full">
        {/* Sleek Dark Header */}
        <div className="modal-header">
          <div className="modal-title">
            {mode === 'login' ? (
              <>
                <LogIn size={20} className="text-indigo-400" />
                <span>Sign In</span>
              </>
            ) : (
              <>
                <UserPlus size={20} className="text-indigo-400" />
                <span>Create Account</span>
              </>
            )}
          </div>
          <button
            type="button"
            className="text-slate-400 hover:text-white p-1 rounded-lg transition-colors bg-transparent border-0 cursor-pointer"
            onClick={onClose}
          >
            <X size={18} />
          </button>
        </div>

        {/* Segmented Toggle: Sign In vs Sign Up */}
        <div className="modal-tabs">
          <button
            type="button"
            className={`modal-tab-btn ${mode === 'login' ? 'active' : ''}`}
            onClick={() => { setMode('login'); setErrorMsg(null); }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`modal-tab-btn ${mode === 'register' ? 'active' : ''}`}
            onClick={() => { setMode('register'); setErrorMsg(null); }}
          >
            Sign Up
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body space-y-4">
          {errorMsg && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-700 text-xs font-medium flex items-start gap-2">
              <AlertTriangle size={16} className="text-red-500 flex-shrink-0 mt-0.5" />
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Full Name (Sign Up only) */}
          {mode === 'register' && (
            <div className="input-group">
              <label htmlFor="fullName">
                <User size={14} className="text-indigo-600" /> Full Name
              </label>
              <input
                id="fullName"
                type="text"
                required
                placeholder="Jane Doe"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />
            </div>
          )}

          {/* Email */}
          <div className="input-group">
            <label htmlFor="email">
              <Mail size={14} className="text-indigo-600" /> Work Email
            </label>
            <input
              id="email"
              type="email"
              required
              placeholder="user@devcompass.ai"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          {/* Password */}
          <div className="input-group">
            <label htmlFor="password">
              <Lock size={14} className="text-indigo-600" /> Password
            </label>
            <input
              id="password"
              type="password"
              required
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          {/* Modal Actions */}
          <div className="modal-actions pt-2">
            <button
              type="submit"
              disabled={loading}
              className="py-2.5 px-4 font-semibold rounded-lg text-sm text-white border-0 transition-all shadow-md bg-indigo-600 hover:bg-indigo-700 shadow-indigo-200 flex items-center justify-center gap-2 cursor-pointer w-full"
            >
              <span>
                {loading
                  ? 'Processing...'
                  : mode === 'login'
                  ? 'Sign In to Account'
                  : 'Create Account'}
              </span>
              <ArrowRight size={16} />
            </button>

            <button
              type="button"
              className="text-xs text-slate-500 hover:text-indigo-600 bg-transparent border-0 cursor-pointer transition-colors text-center py-1 w-full"
              onClick={() => {
                setErrorMsg(null);
                setMode(mode === 'login' ? 'register' : 'login');
              }}
            >
              {mode === 'login'
                ? "Don't have an account? Sign Up"
                : 'Already have an account? Sign In'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
