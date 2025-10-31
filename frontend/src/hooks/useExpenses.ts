import { useCallback, useEffect, useMemo, useState } from 'react';
import { apiClient, routes } from '../lib/api';
import { Expense } from '../types';
import { parseExpenses } from '../utils/expense';

interface ExpenseFilters {
  startDate?: string;
  endDate?: string;
  month?: string;
  year?: string;
}

interface UseExpensesResult {
  expenses: Expense[];
  isLoading: boolean;
  error: string | null;
  refresh: () => void;
  filterByRange: (startDate: string, endDate: string) => void;
  filterByMonth: (month: string, year: string) => void;
}

const formatDateTime = (value: string) => `${value} 00:00:00`;
const formatEndOfDay = (value: string) => `${value} 23:59:59`;

export const useExpenses = (): UseExpensesResult => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filters, setFilters] = useState<ExpenseFilters>({});

  const fetchExpenses = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (filters.startDate && filters.endDate) {
        const response = await apiClient.post(routes.expense.byDate, {
          startedDate: filters.startDate,
          completedDate: filters.endDate
        });
        setExpenses(parseExpenses(response.data));
      } else if (filters.month && filters.year) {
        const response = await apiClient.post(routes.expense.byMonth, {
          month: filters.month,
          year: filters.year
        });
        setExpenses(parseExpenses(response.data));
      } else {
        const response = await apiClient.get(routes.expense.list);
        setExpenses(parseExpenses(response.data));
      }
    } catch (err: any) {
      const message = err?.response?.data ?? err?.message ?? 'Impossibile recuperare le spese';
      setError(typeof message === 'string' ? message : JSON.stringify(message));
      setExpenses([]);
    } finally {
      setIsLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    void fetchExpenses();
  }, [fetchExpenses]);

  const refresh = useCallback(() => {
    setFilters({});
  }, []);

  const filterByRange = useCallback((start: string, end: string) => {
    const formattedStart = formatDateTime(start);
    const formattedEnd = formatEndOfDay(end);
    setFilters({ startDate: formattedStart, endDate: formattedEnd });
  }, []);

  const filterByMonth = useCallback((month: string, year: string) => {
    setFilters({ month, year });
  }, []);

  return useMemo(
    () => ({ expenses, isLoading, error, refresh, filterByRange, filterByMonth }),
    [expenses, isLoading, error, refresh, filterByRange, filterByMonth]
  );
};
