import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';

const CANCELLABLE = ['PAID', 'SELLER_CONFIRMED', 'READY_FOR_PICKUP', 'RIDER_ASSIGNED', 'DELIVERY_FAILED'];

export default function OrdersPage({ token, onUnauthorized }) {
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api('/api/admin/orders', { token }));
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

  function cancel(o) {
    const reason = window.prompt(`Reason for cancelling order #${o.id}. This is recorded and shown to the parties:`);
    if (!reason || !reason.trim()) return;
    api(`/api/admin/orders/${o.id}/cancel`, { method: 'POST', token, body: { reason: reason.trim() } })
      .then(load)
      .catch((err) => setError(err.message));
  }

  const money = (v) => (v == null ? '—' : `UGX ${Number(v).toLocaleString('en-US')}`);

  return (
    <section>
      <div className="page-head">
        <h2>Orders</h2>
        <button className="ghost" onClick={load} disabled={loading}>{loading ? 'Refreshing…' : 'Refresh'}</button>
      </div>

      {error && <p className="error">{error}</p>}

      {items.length > 0 && (
        <table className="table">
          <thead>
            <tr>
              <th>#</th>
              <th>Status</th>
              <th>Buyer / seller / rider</th>
              <th>Items</th>
              <th>Total</th>
              <th>Commission</th>
              <th>Updated</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((o) => (
              <tr key={o.id}>
                <td>{o.id}</td>
                <td>{o.status}</td>
                <td className="small">
                  {o.buyerName} / {o.sellerName} / {o.riderName || 'unassigned'}
                </td>
                <td className="small">
                  {o.items.map((i) => `${i.quantity} x ${i.productName}`).join(', ')}
                </td>
                <td>{money(o.totalUgx)}</td>
                <td>{money(o.commissionUgx)}</td>
                <td>{new Date(o.updatedAt).toLocaleString()}</td>
                <td className="right">
                  {CANCELLABLE.includes(o.status) && (
                    <button className="danger" onClick={() => cancel(o)}>Cancel</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {!error && items.length === 0 && <p className="muted">No orders yet.</p>}
    </section>
  );
}
