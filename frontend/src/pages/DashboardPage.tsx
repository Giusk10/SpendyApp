import { FormEvent, useEffect, useMemo, useState } from 'react';
import {
  fetchExpenses,
  fetchExpensesByDate,
  fetchExpensesByMonth,
  fetchMonthlyAmountOfYear
} from '../api/expense';
import { Expense } from '../types/expense';
import {
  aggregateExpenses,
  buildMonthlySeries,
  formatCurrency,
  monthLabels,
  normalizeExpenses
} from '../utils/expense';
import { SummaryCard } from '../components/SummaryCard';
import { ExpensesTable } from '../components/ExpensesTable';
import { CategoryBreakdown } from '../components/CategoryBreakdown';
import { MonthlyTrendChart } from '../components/MonthlyTrendChart';

const now = new Date();
const currentYear = now.getFullYear().toString();
const currentMonth = String(now.getMonth() + 1).padStart(2, '0');

type ExpenseFilter =
  | { mode: 'all' }
  | { mode: 'month'; month: string; year: string }
  | { mode: 'range'; startedDate: string; completedDate: string };

export const DashboardPage = () => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [monthlyData, setMonthlyData] = useState<Array<{ month: string; value: number }>>([]);
  const [chartYear, setChartYear] = useState(currentYear);
  const [mode, setMode] = useState<ExpenseFilter['mode']>('all');
  const [monthForm, setMonthForm] = useState({ month: currentMonth, year: currentYear });
  const [rangeForm, setRangeForm] = useState({ startedDate: '', completedDate: '' });
  const [filters, setFilters] = useState<ExpenseFilter>({ mode: 'all' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [monthlyError, setMonthlyError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    const loadExpenses = async () => {
      setLoading(true);
      setError(null);

      try {
        let expensesPromise: Promise<Expense[]>;

        if (filters.mode === 'all') {
          expensesPromise = fetchExpenses();
        } else if (filters.mode === 'month') {
          expensesPromise = fetchExpensesByMonth({ month: filters.month, year: filters.year });
        } else {
          expensesPromise = fetchExpensesByDate({
            startedDate: filters.startedDate,
            completedDate: filters.completedDate
          });
        }

        const monthlyPromise = fetchMonthlyAmountOfYear(chartYear);

        const [expensesResult, monthlyResult] = (await Promise.allSettled([
          expensesPromise,
          monthlyPromise
        ])) as [
          PromiseSettledResult<Expense[]>,
          PromiseSettledResult<Record<string, number | string>>
        ];

        if (!isMounted) {
          return;
        }

        if (expensesResult.status === 'fulfilled') {
          const normalized = normalizeExpenses(
            Array.isArray(expensesResult.value) ? expensesResult.value : []
          );
          setExpenses(normalized);
        } else {
          throw expensesResult.reason;
        }

        if (monthlyResult.status === 'fulfilled') {
          setMonthlyError(null);
          setMonthlyData(buildMonthlySeries(monthlyResult.value ?? {}, chartYear));
        } else {
          setMonthlyError(
            monthlyResult.reason?.message ?? 'Grafico mensile non disponibile al momento.'
          );
          setMonthlyData([]);
        }
      } catch (err: any) {
        if (!isMounted) {
          return;
        }
        setError(err?.message ?? 'Impossibile caricare i dati delle spese.');
        setExpenses([]);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadExpenses();

    return () => {
      isMounted = false;
    };
  }, [filters, chartYear]);

  const metrics = useMemo(() => aggregateExpenses(expenses), [expenses]);
  const activeFilterDescription = useMemo(() => {
    if (filters.mode === 'month') {
      const monthIndex = Number(filters.month) - 1;
      const label = monthLabels[monthIndex] ?? filters.month;
      return `Filtrate per ${label} ${filters.year}.`;
    }
    if (filters.mode === 'range') {
      const formatter = new Intl.DateTimeFormat('it-IT');
      try {
        const start = formatter.format(new Date(filters.startedDate));
        const end = formatter.format(new Date(filters.completedDate));
        return `Intervallo selezionato: ${start} → ${end}.`;
      } catch (err) {
        return null;
      }
    }
    return null;
  }, [filters]);

  const handleApplyFilters = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (mode === 'month') {
      setFilters({ mode: 'month', month: monthForm.month, year: monthForm.year });
      return;
    }

    if (mode === 'range') {
      if (!rangeForm.startedDate || !rangeForm.completedDate) {
        setError('Seleziona una data di inizio e di fine per applicare il filtro.');
        return;
      }

      if (new Date(rangeForm.startedDate) > new Date(rangeForm.completedDate)) {
        setError('La data di inizio non può essere successiva alla data di fine.');
        return;
      }

      setFilters({
        mode: 'range',
        startedDate: rangeForm.startedDate,
        completedDate: rangeForm.completedDate
      });
      return;
    }

    setFilters({ mode: 'all' });
  };

  const handleClearFilters = () => {
    setMode('all');
    setFilters({ mode: 'all' });
    setRangeForm({ startedDate: '', completedDate: '' });
    setError(null);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dashboard finanziaria</h1>
          <p className="page-subtitle">
            Monitora automaticamente le spese importate dagli estratti conto e ottieni insight istantanei.
          </p>
        </div>
        <div>
          <label style={{ display: 'block', marginBottom: '0.5rem', color: '#475569', fontWeight: 600 }}>
            Anno di analisi
          </label>
          <select
            value={chartYear}
            onChange={(event) => {
              const selectedYear = event.target.value;
              setChartYear(selectedYear);
              setMonthForm((prev) => ({ ...prev, year: selectedYear }));
            }}
            className="input-field"
            style={{ maxWidth: '200px' }}
          >
            {Array.from({ length: 5 }).map((_, index) => {
              const optionYear = (new Date().getFullYear() - index).toString();
              return (
                <option key={optionYear} value={optionYear}>
                  {optionYear}
                </option>
              );
            })}
          </select>
        </div>
      </div>

      <div className="card" style={{ marginBottom: '1.5rem' }}>
        <form
          className="form-grid"
          style={{
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            alignItems: 'end'
          }}
          onSubmit={handleApplyFilters}
        >
          <div>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Mostra spese</label>
            <select
              className="input-field"
              value={mode}
              onChange={(event) => setMode(event.target.value as ExpenseFilter['mode'])}
            >
              <option value="all">Tutte le spese importate</option>
              <option value="month">Solo un mese specifico</option>
              <option value="range">Intervallo di date</option>
            </select>
          </div>

          {mode === 'month' && (
            <>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Mese</label>
                <select
                  className="input-field"
                  value={monthForm.month}
                  onChange={(event) => setMonthForm((prev) => ({ ...prev, month: event.target.value }))}
                >
                  {monthLabels.map((label, index) => {
                    const value = String(index + 1).padStart(2, '0');
                    return (
                      <option key={value} value={value}>
                        {label}
                      </option>
                    );
                  })}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Anno</label>
                <select
                  className="input-field"
                  value={monthForm.year}
                  onChange={(event) => setMonthForm((prev) => ({ ...prev, year: event.target.value }))}
                >
                  {Array.from({ length: 5 }).map((_, index) => {
                    const optionYear = (new Date().getFullYear() - index).toString();
                    return (
                      <option key={optionYear} value={optionYear}>
                        {optionYear}
                      </option>
                    );
                  })}
                </select>
              </div>
            </>
          )}

          {mode === 'range' && (
            <>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Data inizio</label>
                <input
                  type="date"
                  className="input-field"
                  value={rangeForm.startedDate}
                  onChange={(event) =>
                    setRangeForm((prev) => ({ ...prev, startedDate: event.target.value }))
                  }
                  max={rangeForm.completedDate || undefined}
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Data fine</label>
                <input
                  type="date"
                  className="input-field"
                  value={rangeForm.completedDate}
                  onChange={(event) =>
                    setRangeForm((prev) => ({ ...prev, completedDate: event.target.value }))
                  }
                  min={rangeForm.startedDate || undefined}
                />
              </div>
            </>
          )}

          <div
            style={{
              display: 'flex',
              gap: '0.75rem',
              flexWrap: 'wrap'
            }}
          >
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? 'Aggiornamento…' : 'Applica filtri'}
            </button>
            {filters.mode !== 'all' && (
              <button type="button" className="secondary-button" onClick={handleClearFilters}>
                Azzera filtri
              </button>
            )}
          </div>
        </form>

        {activeFilterDescription && (
          <p style={{ margin: '1rem 0 0', color: '#2563eb', fontWeight: 600 }}>
            {activeFilterDescription}
          </p>
        )}
      </div>

      {error && (
        <div className="alert error" style={{ marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {loading ? (
        <div className="card" style={{ textAlign: 'center' }}>
          <p style={{ color: '#475569' }}>Caricamento dei tuoi dati finanziari…</p>
        </div>
      ) : (
        <>
          <div className="card-grid">
            <SummaryCard
              title="Uscite totali"
              value={formatCurrency(metrics.totalExpenses)}
              helper={`${metrics.totalTransactions} movimenti monitorati`}
              accent="#ef4444"
            />
            <SummaryCard
              title="Spesa media"
              value={formatCurrency(metrics.averageExpense)}
              helper="Calcolata su tutte le transazioni importate"
              accent="#6366f1"
            />
            <SummaryCard
              title="Uscita maggiore"
              value={formatCurrency(metrics.highestExpense)}
              helper="Il movimento più rilevante registrato"
              accent="#0ea5e9"
            />
          </div>

          <div className="metrics-grid" style={{ marginTop: '1.5rem' }}>
            {monthlyError ? (
              <div className="card" style={{ minHeight: '320px' }}>
                <h3 className="section-title">Andamento mensile</h3>
                <p className="section-subtitle">Grafico temporaneamente non disponibile</p>
                <div className="alert error" style={{ marginTop: '1.25rem' }}>
                  {monthlyError}
                </div>
              </div>
            ) : (
              <MonthlyTrendChart data={monthlyData} />
            )}
            <CategoryBreakdown metrics={metrics} />
          </div>

          <div style={{ marginTop: '1.5rem' }}>
            <ExpensesTable expenses={expenses} />
          </div>
        </>
      )}
    </div>
  );
};
