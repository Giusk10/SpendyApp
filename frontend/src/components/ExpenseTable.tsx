import clsx from 'clsx';
import { Expense } from '../types';
import { formatCurrency } from '../utils/expense';
import styles from '../styles/ExpenseTable.module.css';

interface ExpenseTableProps {
  expenses: Expense[];
  isLoading: boolean;
  title: string;
  compact?: boolean;
}

const formatDate = (value: string | null) => {
  if (!value) return '—';
  const date = new Date(value);
  return new Intl.DateTimeFormat('it-IT', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  }).format(date);
};

const ExpenseTable = ({ expenses, isLoading, title, compact }: ExpenseTableProps) => (
  <div className={styles.card}>
    <div className={styles.header}>
      <h2>{title}</h2>
      <span className={styles.count}>{expenses.length} righe</span>
    </div>
    {isLoading ? (
      <p className={styles.placeholder}>Caricamento in corso...</p>
    ) : !expenses.length ? (
      <p className={styles.placeholder}>Nessuna spesa disponibile.</p>
    ) : (
      <div className={styles.tableWrapper}>
        <table className={clsx(styles.table, compact && styles.compact)}>
          <thead>
            <tr>
              <th>Data</th>
              <th>Descrizione</th>
              <th>Categoria</th>
              <th>Importo</th>
            </tr>
          </thead>
          <tbody>
            {expenses.map((expense) => (
              <tr key={expense.id ?? `${expense.description}-${expense.startedDate ?? ''}`}>
                <td>{formatDate(expense.startedDate)}</td>
                <td>
                  <span className={styles.description}>{expense.description || expense.product}</span>
                  <span className={styles.meta}>{expense.type}</span>
                </td>
                <td>
                  <span className={styles.category}>{expense.category}</span>
                </td>
                <td className={styles.amount}>{formatCurrency(expense.amount, expense.currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )}
  </div>
);

export default ExpenseTable;
