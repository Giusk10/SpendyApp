import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

const links = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/expenses/import', label: 'Importa spese' },
  { to: '/house/link', label: 'Collega casa' },
  { to: '/house/roommates', label: 'Coinquilini' }
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
      <div>
        <NavLink to="/dashboard" className="page-title" style={{ color: '#f8fafc', fontSize: '1.5rem' }}>
          Spendy
        </NavLink>
        <p style={{ margin: '0.1rem 0 0', color: 'rgba(226,232,240,0.7)', fontSize: '0.85rem' }}>
          La tua cabina di regia finanziaria intelligente
        </p>
      </div>
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {username && <span style={{ color: 'rgba(226,232,240,0.8)', fontWeight: 500 }}>Ciao, {username}</span>}
          <button className="secondary-button" onClick={handleLogout}>
            Esci
          </button>
        </div>
      </div>
    </nav>
  );
};
