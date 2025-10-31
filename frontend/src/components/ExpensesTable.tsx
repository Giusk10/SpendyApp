import { Expense } from '../types/expense';
import { formatCurrency, formatDateTime } from '../utils/expense';

interface Props {
  expenses: Expense[];
  currency?: string;
}

export const ExpensesTable: React.FC<Props> = ({ expenses, currency = 'EUR' }) => {
  if (!expenses.length) {
    return (
      <div className="card" style={{ textAlign: 'center' }}>
        <p style={{ color: '#475569', margin: 0 }}>Nessuna spesa disponibile. Importa un estratto conto per iniziare.</p>
      </div>
    );
  }

  return (
    <div className="card table-container">
      <div className="flex-between">
        <div>
          <h3 className="section-title" style={{ marginBottom: '0.35rem' }}>
            Movimenti recenti
          </h3>
          <p className="section-subtitle" style={{ marginBottom: 0 }}>
            Ultimi {Math.min(expenses.length, 10)} movimenti importati
          </p>
        </div>
      </div>
      <table className="table" style={{ marginTop: '1rem' }}>
        <thead>
          <tr>
            <th>Data</th>
            <th>Descrizione</th>
            <th>Categoria</th>
            <th>Importo</th>
            <th>Stato</th>
          </tr>
        </thead>
        <tbody>
          {expenses.slice(0, 10).map((expense) => (
            <tr key={expense.id ?? `${expense.description}-${expense.startedDate}`}>
              <td>{formatDateTime(expense.startedDate)}</td>
              <td>{expense.description}</td>
              <td>
                <span className="badge category">{expense.category ?? '—'}</span>
              </td>
              <td>
                <span className="badge negative">{formatCurrency(Math.abs(expense.amount), currency)}</span>
              </td>
              <td>{expense.state ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
