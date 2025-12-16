import { FormEvent, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { register as registerRequest } from '../api/auth';

export const RegisterPage = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    username: '',
    name: '',
    surname: '',
    email: '',
    password: ''
  });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const updateField = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    if (!form.username || !form.password || !form.email) {
      setError('Compila almeno username, email e password per continuare.');
      return;
    }

    setLoading(true);
    try {
      await registerRequest(form);
      setSuccess('Registrazione completata! Ora puoi accedere.');
      setTimeout(() => navigate('/login'), 1500);
    } catch (err: any) {
      setError(err?.message ?? 'Registrazione non riuscita. Riprova.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="card" style={{ width: '100%', maxWidth: '640px' }}>
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <h1 className="page-title" style={{ marginBottom: '0.5rem' }}>
            Crea il tuo account Spendy
          </h1>
          <p className="page-subtitle">
            Collabora con i tuoi coinquilini, importa estratti conto e visualizza dashboard intelligenti.
          </p>
        </div>

        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}

        {success && (
          <div className="alert" style={{ backgroundColor: 'var(--success-50)', color: 'var(--success-700)', border: '1px solid var(--success-500)' }} role="status">
            {success}
          </div>
        )}

        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.5rem' }}>
            <div className="input-group">
              <label className="input-label">Nome</label>
              <input
                className="input-field"
                placeholder="Giulia"
                value={form.name}
                onChange={(event) => updateField('name', event.target.value)}
                autoComplete="given-name"
              />
            </div>
            <div className="input-group">
              <label className="input-label">Cognome</label>
              <input
                className="input-field"
                placeholder="Rossi"
                value={form.surname}
                onChange={(event) => updateField('surname', event.target.value)}
                autoComplete="family-name"
              />
            </div>
          </div>

          <div className="input-group">
            <label className="input-label">Username</label>
            <input
              className="input-field"
              placeholder="giulia.rossi"
              value={form.username}
              onChange={(event) => updateField('username', event.target.value)}
              autoComplete="username"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Email</label>
            <input
              className="input-field"
              type="email"
              placeholder="giulia@spendy.it"
              value={form.email}
              onChange={(event) => updateField('email', event.target.value)}
              autoComplete="email"
            />
          </div>

          <div className="input-group">
            <label className="input-label">Password</label>
            <input
              className="input-field"
              type="password"
              placeholder="Almeno 8 caratteri"
              value={form.password}
              onChange={(event) => updateField('password', event.target.value)}
              autoComplete="new-password"
            />
          </div>

          <button type="submit" className="primary-button" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
            {loading ? 'Registrazione in corso…' : 'Crea account'}
          </button>
        </form>

        <p style={{ marginTop: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.9rem' }}>
          Hai già un account?{' '}
          <span
            onClick={() => navigate('/login')}
            style={{ color: 'var(--primary-600)', fontWeight: 600, cursor: 'pointer' }}
          >
            Accedi
          </span>
        </p>
      </div>
    </div>
  );
};
