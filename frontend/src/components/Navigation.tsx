import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/expenses/import', label: 'Importa spese' },
];

export const Navigation = () => {
  const { isAuthenticated, username, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <NavLink to="/dashboard" className="nav-brand">
          Spendy
        </NavLink>
        <div className="nav-links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
            >
              {link.label}
            </NavLink>
          ))}
          <div className="flex-between" style={{ gap: '1rem' }}>
            {username && <span style={{ color: 'var(--slate-600)', fontWeight: 500, fontSize: '0.9rem' }}>Ciao, {username}</span>}
            <button className="secondary-button" onClick={handleLogout} style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}>
              Esci
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};
