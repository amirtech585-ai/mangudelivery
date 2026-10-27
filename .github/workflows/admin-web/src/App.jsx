import { useState } from 'react';
import LoginPage from './pages/LoginPage';
import ApplicationsPage from './pages/ApplicationsPage';
import UsersPage from './pages/UsersPage';
import AuditPage from './pages/AuditPage';
import OrdersPage from './pages/OrdersPage';

const PAGES = [
  { id: 'applications', label: 'Applications' },
  { id: 'orders', label: 'Orders' },
  { id: 'accounts', label: 'Accounts' },
  { id: 'audit', label: 'Audit log' },
];

/**
 * The session is held in memory only: closing the tab logs the administrator out.
 * This avoids storing an admin token in localStorage.
 */
export default function App() {
  const [session, setSession] = useState(null);
  const [page, setPage] = useState('applications');

  if (!session) {
    return <LoginPage onLogin={setSession} />;
  }

  const onUnauthorized = () => setSession(null);
  const props = { token: session.token, onUnauthorized };

  return (
    <div className="shell">
      <header className="topbar">
        <span className="brand">ManguDelivery Admin</span>
        <button className="ghost" onClick={() => setSession(null)}>Log out</button>
      </header>
      <nav className="tabs">
        {PAGES.map((p) => (
          <button key={p.id} className={p.id === page ? 'tab active' : 'tab'} onClick={() => setPage(p.id)}>
            {p.label}
          </button>
        ))}
      </nav>
      <main className="content">
        {page === 'applications' && <ApplicationsPage {...props} />}
        {page === 'orders' && <OrdersPage {...props} />}
        {page === 'accounts' && <UsersPage {...props} />}
        {page === 'audit' && <AuditPage {...props} />}
      </main>
    </div>
  );
}
