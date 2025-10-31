import { ChangeEvent, FormEvent, useState } from 'react';
import { uploadExpenseFile } from '../api/expense';

export const UploadPage = () => {
  const [file, setFile] = useState<File | null>(null);
  const [status, setStatus] = useState<{ type: 'success' | 'error'; message: string } | null>(null);
  const [loading, setLoading] = useState(false);

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const selected = event.target.files?.[0];
    if (!selected) {
      setFile(null);
      return;
    }

    if (!selected.name.endsWith('.csv')) {
      setStatus({ type: 'error', message: 'Carica un file CSV esportato dal tuo estratto conto.' });
      setFile(null);
      return;
    }

    setStatus(null);
    setFile(selected);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setStatus(null);

    if (!file) {
      setStatus({ type: 'error', message: 'Seleziona prima un file CSV.' });
      return;
    }

    setLoading(true);
    try {
      const response = await uploadExpenseFile(file);
      const message = typeof response === 'string' ? response : 'File importato con successo!';
      setStatus({ type: 'success', message });
      setFile(null);
    } catch (err: any) {
      setStatus({ type: 'error', message: err?.message ?? 'Importazione non riuscita.' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Importa un estratto conto</h1>
          <p className="page-subtitle">
            Carica il CSV esportato dalla tua banca: Spendy classificherà le spese e aggiornerà automaticamente la dashboard.
          </p>
        </div>
      </div>

      <div className="card" style={{ maxWidth: '640px' }}>
        {status && <div className={`alert ${status.type}`}>{status.message}</div>}

        <form className="form-grid" onSubmit={handleSubmit}>
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>File CSV</label>
            <input
              type="file"
              accept=".csv"
              className="input-field"
              onChange={handleFileChange}
            />
            <p style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#64748b' }}>
              Suggerimento: quasi tutte le banche permettono di esportare l'estratto conto in formato CSV. Assicurati che sia in UTF-8.
            </p>
          </div>

          <button className="primary-button" type="submit" disabled={loading || !file}>
            {loading ? 'Importazione in corso…' : 'Importa estratto conto'}
          </button>
        </form>
      </div>
    </div>
  );
};
