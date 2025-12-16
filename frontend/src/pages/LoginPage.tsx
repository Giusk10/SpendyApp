import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login as loginRequest } from '../api/auth';
import { useAuth } from '../hooks/useAuth';

export const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!identifier || !password) {
      setError('Inserisci le tue credenziali complete per continuare.');
      return;
    }

    setLoading(true);
    try {
      const payload = identifier.includes('@')
        ? { email: identifier, password }
        : { username: identifier, password };

      const response = await loginRequest(payload);
      login(response.token, identifier);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err?.message ?? 'Accesso non riuscito. Controlla le credenziali e riprova.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="card" style={{ width: '100%', maxWidth: '480px' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 className="page-title" style={{ marginBottom: '0.5rem' }}>
            Bentornato su Spendy
          </h1>
          <p className="page-subtitle">
            Gestisci in modo smart i conti condivisi e tieni tutto sotto controllo.
          </p>
        </div>

        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}

        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="input-group">
            <label htmlFor="identifier" className="input-label">
              Username o email
            </label>
            <input
              id="identifier"
              className="input-field"
              placeholder="es. giulia.rossi o giulia@spendy.it"
              value={identifier}
              onChange={(event) => setIdentifier(event.target.value)}
              autoComplete="username"
            />
          </div>

          <div className="input-group">
            <label htmlFor="password" className="input-label">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="input-field"
              placeholder="La tua password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
          </div>

          <button type="submit" className="primary-button" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
            {loading ? 'Accesso in corso…' : 'Accedi'}
          </button>
        </form>

        <p style={{ marginTop: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Non hai ancora un account?{' '}
          <span
            onClick={() => navigate('/register')}
            style={{ color: 'var(--primary-600)', fontWeight: 600, cursor: 'pointer' }}
          >
            Registrati ora
          </span>
        </p>
      </div>
    </div>
  );
};
