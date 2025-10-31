import { FormEvent, useState } from 'react';
import { getCoinquilini } from '../api/auth';

interface Coinquilino {
  id?: string;
  username?: string;
  name?: string;
  surname?: string;
  email?: string;
}

export const CoinquiliniPage = () => {
  const [houseId, setHouseId] = useState('');
  const [coinquilini, setCoinquilini] = useState<Coinquilino[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(null);
    setCoinquilini([]);

    if (!houseId) {
      setError('Inserisci l\'identificativo della casa.');
      return;
    }

    setLoading(true);
    try {
      const response = await getCoinquilini(houseId);
      setCoinquilini(response);
      if (!response.length) {
        setError('Nessun coinquilino trovato per questa casa.');
      }
    } catch (err: any) {
      setError(err?.message ?? 'Impossibile recuperare i coinquilini.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Coinquilini della tua casa</h1>
          <p className="page-subtitle">
            Inserisci l'ID della casa per visualizzare chi è collegato e mantenere tutti allineati.
          </p>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '2rem' }}>
        <form className="form-grid" onSubmit={handleSubmit} style={{ alignItems: 'end' }}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>ID casa</label>
            <input
              className="input-field"
              placeholder="Es. 652c0f12a9"
              value={houseId}
              onChange={(event) => setHouseId(event.target.value)}
            />
          </div>
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Ricerca in corso…' : 'Recupera coinquilini'}
          </button>
        </form>
        {error && <div className="alert error" style={{ marginTop: '1rem' }}>{error}</div>}
      </div>

      {coinquilini.length > 0 && (
        <div className="card">
          <h3 className="section-title">Coinquilini collegati</h3>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Username</th>
                  <th>Email</th>
                </tr>
              </thead>
              <tbody>
                {coinquilini.map((user) => (
                  <tr key={user.id ?? user.email ?? user.username}>
                    <td>{[user.name, user.surname].filter(Boolean).join(' ') || '—'}</td>
                    <td>{user.username ?? '—'}</td>
                    <td>{user.email ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
