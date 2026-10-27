import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';

export default function UsersPage({ token, onUnauthorized }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api('/api/admin/users', { token }));
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

  async function run(action) {
    try {
      await action();
      await load();
    } catch (err) {
      setError(err.message);
    }
  }

  function suspend(u) {
    const reason = window.prompt(`Reason for suspending ${u.fullName} (${u.role}). This is recorded in the audit log:`);
    if (reason && reason.trim()) {
      run(() => api(`/api/admin/users/${u.id}/suspend`, { method: 'POST', token, body: { reason: reason.trim() } }));
    }
  }

  function reinstate(u) {
    if (window.confirm(`Reinstate ${u.fullName} (${u.role})?`)) {
      run(() => api(`/api/admin/users/${u.id}/reinstate`, { method: 'POST', token }));
    }
  }

  return (
    <section>
      <div className="page-head">
        <h2>Accounts</h2>
        <button className="ghost" onClick={load} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</button>
      </div>

      {error && <p className="error">{error}</p>}

      {items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th>Joined</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.id}>
                <td>{u.fullName}</td>
                <td>{u.email}</td>
                <td><span className={`badge ${u.role.toLowerCase()}`}>{u.role}</span></td>
                <td>{u.status}</td>
                <td>{new Date(u.createdAt).toLocaleDateString()}</td>
                <td className="right">
                  {u.role !== 'ADMIN' && u.status !== 'SUSPENDED' && (
                    <button className="danger" onClick={() => suspend(u)}>Suspend</button>
                  )}
                  {u.role !== 'ADMIN' && u.status === 'SUSPENDED' && (
                    <button onClick={() => reinstate(u)}>Reinstate</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!error && items.length === 0 && <p className="muted">No accounts yet.</p>}
    </section>
  );
}
