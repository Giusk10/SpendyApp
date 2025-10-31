import { FormEvent, useState } from 'react';
import { linkHouse } from '../api/auth';

export const HouseLinkPage = () => {
  const [houseCode, setHouseCode] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setStatus(null);

    if (!houseCode) {
      setStatus({ type: 'error', message: 'Inserisci un codice casa valido.' });
      return;
    }

    setLoading(true);
    try {
      const response = await linkHouse(houseCode);
      const message = typeof response === 'string' ? response : response?.message ?? 'Casa collegata!';
      setStatus({ type: 'success', message });
      setHouseCode('');
    } catch (err: any) {
      setStatus({ type: 'error', message: err?.message ?? 'Impossibile collegare la casa.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Collega una casa condivisa</h1>
          <p className="page-subtitle">Inserisci il codice ricevuto via email per sincronizzare il tuo profilo.</p>
        </div>
      </div>

      <div className="card" style={{ maxWidth: '540px' }}>
        {status && (
          <div className={`alert ${status.type}`}>{status.message}</div>
        )}

        <form className="form-grid" onSubmit={handleSubmit}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Codice casa</label>
            <input
              className="input-field"
              placeholder="Es. CASA-1234"
              value={houseCode}
              onChange={(event) => setHouseCode(event.target.value.toUpperCase())}
            />
          </div>
          <button className="primary-button" type="submit" disabled={loading}>
            {loading ? 'Collegamento in corso…' : 'Collega casa'}
          </button>
        </form>

        <div className="card-divider" />
        <div>
          <h3 style={{ margin: '0 0 0.5rem', color: '#0f172a' }}>Come funziona?</h3>
          <ol style={{ margin: 0, paddingLeft: '1.25rem', color: '#475569', lineHeight: 1.8 }}>
            <li>Richiedi al referente della casa il codice di invito.</li>
            <li>Inseriscilo qui per collegare il tuo account alla casa.</li>
            <li>Una volta collegato potrai vedere i coinquilini e condividere le spese.</li>
          </ol>
        </div>
      </div>
    </div>
  );
};
