import { addMonths, format, parseISO } from 'date-fns';
import { Expense } from '../types';

export const parseExpense = (expense: any): Expense => ({
  id: expense.id,
  type: expense.type ?? '',
  product: expense.product ?? '',
  startedDate: expense.startedDate ?? null,
  completedDate: expense.completedDate ?? null,
  description: expense.description ?? '',
  amount: Number(expense.amount ?? 0),
  fee: Number(expense.fee ?? 0),
  currency: expense.currency ?? 'EUR',
  state: expense.state ?? '',
  category: expense.category ?? 'Uncategorized'
});

export const parseExpenses = (data: any): Expense[] => {
  if (!Array.isArray(data)) {
    return [];
  }
  return data.map(parseExpense);
};

export const calculateSummary = (expenses: Expense[]) => {
  if (!expenses.length) {
    return {
      total: 0,
      average: 0,
      categories: {},
      latest: [] as Expense[],
      trend: [] as { month: string; amount: number }[],
      currency: 'EUR'
    };
  }

  const totalsByCategory: Record<string, number> = {};
  const sortedByDate = [...expenses].sort((a, b) => {
    const aTime = a.startedDate ? new Date(a.startedDate).getTime() : 0;
    const bTime = b.startedDate ? new Date(b.startedDate).getTime() : 0;
    return bTime - aTime;
  });

  const total = expenses.reduce((sum, expense) => sum + expense.amount, 0);

  expenses.forEach((expense) => {
    const key = expense.category ?? 'Uncategorized';
    totalsByCategory[key] = (totalsByCategory[key] ?? 0) + expense.amount;
  });

  const currency = expenses[0]?.currency ?? 'EUR';

  const lastSixMonths = Array.from({ length: 6 }).map((_, index) => {
    const date = addMonths(new Date(), -index);
    const key = format(date, 'yyyy-MM');
    const amount = expenses
      .filter((expense) => expense.startedDate && format(new Date(expense.startedDate), 'yyyy-MM') === key)
      .reduce((sum, expense) => sum + expense.amount, 0);
    return { month: key, amount };
  }).reverse();

  return {
    total,
    average: total / expenses.length,
    categories: totalsByCategory,
    latest: sortedByDate.slice(0, 5),
    trend: lastSixMonths,
    currency
  };
};

export const formatCurrency = (value: number, currency = 'EUR') =>
  new Intl.NumberFormat('it-IT', {
    style: 'currency',
    currency
  }).format(value);

export const monthLabels = (data: Record<string, number>) =>
  Object.keys(data)
    .sort()
    .map((monthKey) => {
      const [year, month] = monthKey.split('-');
      const date = parseISO(`${year}-${month.padStart(2, '0')}-01`);
      return format(date, 'MMM yyyy');
    });

export const monthValues = (data: Record<string, number>) =>
  Object.keys(data)
    .sort()
    .map((key) => data[key]);
