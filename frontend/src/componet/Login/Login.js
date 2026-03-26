import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

const FEATURES = [
  { icon: '🧠', text: 'AI-powered quiz & flashcard generator' },
  { icon: '📅', text: 'Smart assignment tracker & calendar' },
  { icon: '💼', text: 'Career tools, resume builder & jobs board' },
  { icon: '💬', text: '24/7 AI chatbot & support tickets' },
];

function Login() {
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw]     = useState(false);
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');
  const navigate  = useNavigate();
  const location  = useLocation();
  const from = location.state?.from?.pathname || '/dashboard';

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await axios.post('http://localhost:8080/login', { email, password });
      localStorage.setItem('userId',   data.id);
      localStorage.setItem('userName', data.name || '');
      localStorage.setItem('userRole', data.role || 'STUDENT');
      if (data.profilePhoto) localStorage.setItem('userProfilePhoto', data.profilePhoto);
      else localStorage.removeItem('userProfilePhoto');
      if (data.year != null) localStorage.setItem('userYear', String(data.year));
      else localStorage.removeItem('userYear');
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid email or password.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="al-wrap">
      {/* ── Left Panel ── */}
      <div className="al-panel al-left">
        <div className="al-left-inner">
          <Link to="/" className="al-logo">
            <span className="al-logo-icon">◆</span>
            <span>Student Hub</span>
          </Link>
          <div className="al-left-content">
            <h2>Everything you need to succeed — in one place.</h2>
            <p>Join thousands of students, alumni and administrators using Student Hub to manage their academic journey.</p>
            <ul className="al-feature-list">
              {FEATURES.map((f) => (
                <li key={f.text}>
                  <span className="al-feat-icon">{f.icon}</span>
                  <span>{f.text}</span>
                </li>
              ))}
            </ul>
          </div>
          <div className="al-left-img">
            <img
              src="https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=700&q=80"
              alt="Students studying"
            />
            <div className="al-left-img-overlay" />
          </div>
        </div>
      </div>

      {/* ── Right Panel ── */}
      <div className="al-panel al-right">
        <div className="al-form-wrap">
          {/* Top link */}
          <p className="al-top-link">
            New here? <Link to="/register">Create an account →</Link>
          </p>

          <div className="al-form-head">
            <div className="al-form-icon">👋</div>
            <h1>Welcome back</h1>
            <p>Sign in to your Student Hub account</p>
          </div>

          {error && (
            <div className="al-error" role="alert">
              <span>⚠️</span> {error}
            </div>
          )}

          <form onSubmit={onSubmit} className="al-form" noValidate>
            <div className="al-field">
              <label htmlFor="login-email">Email address</label>
              <div className="al-input-wrap">
                <span className="al-input-icon">✉️</span>
                <input
                  id="login-email"
                  type="email"
                  placeholder="you@example.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="email"
                />
              </div>
            </div>

            <div className="al-field">
              <label htmlFor="login-password">Password</label>
              <div className="al-input-wrap">
                <span className="al-input-icon">🔒</span>
                <input
                  id="login-password"
                  type={showPw ? 'text' : 'password'}
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  className="al-pw-toggle"
                  onClick={() => setShowPw((p) => !p)}
                  aria-label="Toggle password visibility"
                >
                  {showPw ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            <button type="submit" className="al-submit" disabled={loading}>
              {loading ? (
                <span className="al-spinner" />
              ) : (
                <>Sign In <span>→</span></>
              )}
            </button>
          </form>

          <div className="al-divider"><span>or continue with</span></div>

          <div className="al-social-row">
            <button type="button" className="al-social-btn">
              <svg width="18" height="18" viewBox="0 0 24 24"><path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/><path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/><path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/><path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/></svg>
              Google
            </button>
            <button type="button" className="al-social-btn">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="#1877F2"><path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/></svg>
              Facebook
            </button>
          </div>

          <p className="al-bottom-note">
            Don't have an account? <Link to="/register">Sign up free</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default Login;
