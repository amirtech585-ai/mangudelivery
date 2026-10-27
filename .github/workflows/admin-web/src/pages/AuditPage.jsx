import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';

export default function AuditPage({ token, onUnauthorized }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api('/api/admin/audit', { token }));
      setError('');
    } catch (err) {
      if (err.status === 401 || err.status === 403) return onUnauthorized();
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [token, onUnauthorized]);

  useEffect(() => {
    load();
    const id = setInterval(load, 15000);
    return () => clearInterval(id);
  }, [load]);

  return (
    <section>
      <div className="page-head">
        <h2>Audit log</h2>
        <button className="ghost" onClick={load} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</button>
      </div>

      {error && <p className="error">{error}</p>}

      {items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>When</th>
              <th>Action</th>
              <th>Target</th>
              <th>Details</th>
              <th>Actor</th>
            </tr>
          </thead>
          <tbody>
            {items.map((a) => (
              <tr key={a.id}>
                <td>{new Date(a.createdAt).toLocaleString()}</td>
                <td>{a.action}</td>
                <td>{a.targetType} #{a.targetId}</td>
                <td className="small">{a.details}</td>
                <td>#{a.actorId}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!error && items.length === 0 && <p className="muted">No audit entries yet.</p>}
    </section>
  );
}
