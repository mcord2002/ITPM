import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import './Inquiry.css';

const API = 'http://localhost:8080/api';

function Inquiry() {
  const userId = localStorage.getItem('userId');
  const [question, setQuestion] = useState('');
  const [chatResult, setChatResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showTicketForm, setShowTicketForm] = useState(false);
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketDesc, setTicketDesc] = useState('');
  const [ticketSent, setTicketSent] = useState(false);

  const handleAsk = async (e) => {
    e.preventDefault();
    if (!question.trim()) return;
    setLoading(true);
    setChatResult(null);
    setTicketSent(false);
    try {
      const res = await axios.post(`${API}/inquiry/ask`, {
        q: question.trim(),
        userId: Number(userId),
        autoTicket: true,
      });
      setChatResult(res.data || null);
    } catch (e) {
      console.error(e);
      setChatResult({
        answered: false,
        answers: [],
        ticketCreated: false,
        message: 'Could not reach chatbot service.',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCreateTicket = async (e) => {
    e.preventDefault();
    if (!userId || !ticketSubject.trim() || !ticketDesc.trim()) return;
    try {
      await axios.post(`${API}/tickets`, {
        userId: Number(userId),
        subject: ticketSubject.trim(),
        description: ticketDesc.trim(),
        status: 'OPEN',
      });
      setTicketSent(true);
      setShowTicketForm(false);
      setTicketSubject('');
      setTicketDesc('');
    } catch (e) {
      alert('Failed to create ticket');
    }
  };

  if (!userId) {
    return (
      <div className="inquiry-page">
        <div className="container"><p className="muted">Please log in to ask questions.</p><Link to="/login">Login</Link></div>
      </div>
    );
  }

  return (
    <div className="inquiry-page">
      <div className="container">
        <header className="page-header">
          <h1>🤖 Ask the Chatbot</h1>
          <p>University help desk chatbot for student questions. If AI cannot answer, a support ticket is created automatically.</p>
        </header>

        <form onSubmit={handleAsk} className="card ask-form">
          <input
            type="text"
            placeholder="e.g. How do I register? What are the exam rules?"
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            className="ask-input"
            disabled={loading}
          />
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Chatbot thinking…' : 'Ask Chatbot'}
          </button>
        </form>

        {chatResult !== null && (
          <section className="results-section">
            {chatResult.answered ? (
              <>
                <h2 className="results-title">💬 Chatbot Response ({(chatResult.answers || []).length})</h2>
                <div className="results-list">
                  {(chatResult.answers || []).map((k) => (
                    <article key={k.id} className="card result-card">
                      <span className="result-category">{k.category || 'General'}</span>
                      <h3>{k.title}</h3>
                      <div className="result-content">{k.content}</div>
                    </article>
                  ))}
                </div>
                <p className="muted">Still need help? <button type="button" className="link-btn" onClick={() => setShowTicketForm(true)}>Create a support ticket</button></p>
              </>
            ) : (
              <div className="card no-results">
                <h3>Chatbot couldn't find an answer</h3>
                <p>{chatResult.message || 'No matching training data found.'}</p>
                {chatResult.ticketCreated ? (
                  <p className="success-msg">Support ticket auto-created (ID: #{chatResult.ticketId}). Admin will respond soon.</p>
                ) : (!showTicketForm && !ticketSent && (
                  <button type="button" className="btn btn-primary" onClick={() => setShowTicketForm(true)}>
                    Create support ticket
                  </button>
                ))}
                {ticketSent && <p className="success-msg">Ticket created. We&apos;ll get back to you.</p>}
              </div>
            )}
          </section>
        )}

        {showTicketForm && !ticketSent && (
          <form onSubmit={handleCreateTicket} className="card ticket-form">
            <h3>Create support ticket</h3>
            <input placeholder="Subject" value={ticketSubject} onChange={(e) => setTicketSubject(e.target.value)} required />
            <textarea placeholder="Describe your issue or question…" value={ticketDesc} onChange={(e) => setTicketDesc(e.target.value)} rows={5} required />
            <div className="form-actions">
              <button type="submit" className="btn btn-primary">Submit ticket</button>
              <button type="button" className="btn btn-ghost" onClick={() => setShowTicketForm(false)}>Cancel</button>
            </div>
          </form>
        )}

        <div className="page-links">
          <Link to="/knowledge-base" className="btn btn-ghost">Knowledge Base</Link>
          <Link to="/tickets" className="btn btn-ghost">My Tickets</Link>
          <Link to="/dashboard" className="btn btn-primary">Dashboard</Link>
        </div>
      </div>
    </div>
  );
}

export default Inquiry;
