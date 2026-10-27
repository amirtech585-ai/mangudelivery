import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';

const TABS = ['PENDING', 'APPROVED', 'REJECTED'];

const DETAIL_LABELS = {
  businessName: 'Business name',
  productCategories: 'Product categories',
  nationalId: 'National ID',
  licenceNumber: 'Driving licence',
  vehicleType: 'Vehicle type',
  vehicleRegistration: 'Vehicle registration',
  location: 'Location',
};

export default function ApplicationsPage({ token, onUnauthorized }) {
  const [tab, setTab] = useState('PENDING');
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api(`/api/admin/applications?status=${tab}`, { token }));
      setError('');
    } catch (err) {
      if (err.status === 401 || err.status === 403) return onUnauthorized();
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [tab, token, onUnauthorized]);

  // Near-real-time: poll every 15 seconds until WebSocket events are added (Phase 9).
  useEffect(() => {
    load();
    const id = setInterval(load, 15000);
    return () => clearInterval(id);
  }, [load]);

  async function act(id, action, body) {
    try {
      await api(`/api/admin/applications/${id}/${action}`, { method: 'POST', token, body });
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  function approve(item) {
    if (window.confirm(`Approve ${item.fullName} as ${item.role}?`)) {
      act(item.id, 'approve');
    }
  }

  function reject(item) {
    const reason = window.prompt('Reason for rejection (shown to the applicant):');
    if (reason && reason.trim()) {
      act(item.id, 'reject', { reason: reason.trim() });
    }
  }

  return (
    <section>
      <div className="page-head">
        <h2>Partner applications</h2>
        <button className="ghost" onClick={load} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</button>
      </div>

      <nav className="tabs">
        {TABS.map((t) => (
          <button key={t} className={t === tab ? 'tab active' : 'tab'} onClick={() => setTab(t)}>
            {t.charAt(0) + t.slice(1).toLowerCase()}
          </button>
        ))}
      </nav>

      {error && <p className="error">{error}</p>}
      {!error && items.length === 0 && <p className="muted">No {tab.toLowerCase()} applications.</p>}

      <div className="grid">
        {items.map((item) => (
          <article key={item.id} className="card">
            <div className="row">
              <strong>{item.fullName}</strong>
              <span className={`badge ${item.role.toLowerCase()}`}>{item.role}</span>
            </div>
            <p className="muted">{item.email}{item.phone ? ` · ${item.phone}` : ''}</p>
            <p className="muted small">Submitted {new Date(item.submittedAt).toLocaleString()}</p>

            <dl className="details">
              {Object.entries(item.details || {})
                .filter(([, v]) => v)
                .map(([k, v]) => (
                  <div key={k}>
                    <dt>{DETAIL_LABELS[k] || k}</dt>
                    <dd>{v}</dd>
                  </div>
                ))}
            </dl>

            {item.rejectionReason && <p className="error">Rejected: {item.rejectionReason}</p>}

            {item.status === 'PENDING' && (
              <div className="actions">
                <button onClick={() => approve(item)}>Approve</button>
                <button className="danger" onClick={() => reject(item)}>Reject</button>
              </div>
            )}
          </article>
        ))}
      </div>
    </section>
  );
}
