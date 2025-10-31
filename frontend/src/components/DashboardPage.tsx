import { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useExpenses } from '../hooks/useExpenses';
import { apiClient, routes } from '../lib/api';
import { MonthlyAmountResponse } from '../types';
import { calculateSummary, formatCurrency } from '../utils/expense';
import FileUploadCard from './FileUploadCard';
import DateRangeFilters from './DateRangeFilters';
import ExpenseTable from './ExpenseTable';
import MonthlyTrendChart from './MonthlyTrendChart';
import CategoryBreakdownChart from './CategoryBreakdownChart';
import styles from '../styles/DashboardPage.module.css';

const currentYear = new Date().getFullYear();

const DashboardPage = () => {
  const { logout, username } = useAuth();
  const { expenses, isLoading, error, refresh, filterByRange, filterByMonth } = useExpenses();
  const [year, setYear] = useState<string>(String(currentYear));
  const [monthlyData, setMonthlyData] = useState<MonthlyAmountResponse>({});
  const [monthlyLoading, setMonthlyLoading] = useState(false);
  const [monthlyError, setMonthlyError] = useState<string | null>(null);

  const summary = useMemo(() => calculateSummary(expenses), [expenses]);

  useEffect(() => {
    const loadMonthly = async () => {
      setMonthlyLoading(true);
      setMonthlyError(null);
      try {
        const response = await apiClient.post(routes.expense.monthlyAmount, { year });
        setMonthlyData(response.data ?? {});
      } catch (err: any) {
        const message = err?.response?.data ?? err?.message ?? 'Impossibile recuperare i dati mensili';
        setMonthlyError(typeof message === 'string' ? message : JSON.stringify(message));
        setMonthlyData({});
      } finally {
        setMonthlyLoading(false);
      }
    };

    void loadMonthly();
  }, [year, expenses.length]);

  const handleYearChange = (newYear: string) => {
    setYear(newYear);
  };

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <div>
          <h1 className={styles.heading}>Benvenuto, {username ?? 'utente'} 👋</h1>
          <p className={styles.subtitle}>Analizza le tue spese e importa nuovi movimenti in pochi click.</p>
        </div>
        <button className={styles.logout} type="button" onClick={logout}>
          Esci
        </button>
      </header>

      <section className={styles.grid}>
        <FileUploadCard onUploadSuccess={refresh} />
        <div className={styles.summaryCard}>
          <h2>Riepilogo rapido</h2>
          <div className={styles.summaryGrid}>
            <div>
              <p className={styles.summaryLabel}>Totale spese</p>
              <p className={styles.summaryValue}>{formatCurrency(summary.total, summary.currency)}</p>
            </div>
            <div>
              <p className={styles.summaryLabel}>Spesa media</p>
              <p className={styles.summaryValue}>{formatCurrency(summary.average, summary.currency)}</p>
            </div>
            <div>
              <p className={styles.summaryLabel}>Transazioni</p>
              <p className={styles.summaryValue}>{expenses.length}</p>
            </div>
            <div>
              <p className={styles.summaryLabel}>Categoria principale</p>
              <p className={styles.summaryValue}>
                {Object.keys(summary.categories).length
                  ? Object.entries(summary.categories).sort((a, b) => b[1] - a[1])[0][0]
                  : 'N/D'}
              </p>
            </div>
          </div>
        </div>
      </section>

      <section className={styles.filters}>
        <DateRangeFilters onRange={filterByRange} onMonth={filterByMonth} onReset={refresh} />
      </section>

      {error && <div className={styles.alert}>{error}</div>}

      <section className={styles.analytics}>
        <MonthlyTrendChart
          data={monthlyData}
          isLoading={monthlyLoading}
          error={monthlyError}
          onYearChange={handleYearChange}
          selectedYear={year}
        />
        <CategoryBreakdownChart categories={summary.categories} total={summary.total} />
      </section>

      <ExpenseTable expenses={summary.latest} isLoading={isLoading} title="Ultime transazioni" />
      <ExpenseTable expenses={expenses} isLoading={isLoading} title="Tutte le spese" compact />
    </div>
  );
};

export default DashboardPage;
