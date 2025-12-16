import { AggregatedExpenseMetrics, Expense } from '../types/expense';

const parseAmount = (value: number | string | undefined) => {
  if (value === undefined || value === null) {
    return 0;
  }
  if (typeof value === 'number') {
    return value;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? 0 : parsed;
};

export const normalizeExpenses = (rawExpenses: any[]): Expense[] => {
  return rawExpenses.map((expense) => ({
    ...expense,
    amount: parseAmount(expense.amount),
    fee: parseAmount(expense.fee),
    startedDate: expense.startedDate ?? expense.started_date ?? expense.started_date_time,
    completedDate: expense.completedDate ?? expense.completed_date ?? expense.completed_date_time
  }));
};

export const aggregateExpenses = (expenses: Expense[]): AggregatedExpenseMetrics => {
  if (!expenses.length) {
    return {
      totalExpenses: 0,
      averageExpense: 0,
      highestExpense: 0,
      totalTransactions: 0,
      categories: []
    };
  }
  // Consider only outflows (negative amounts) as "uscite". Many banks provide
  // expenses as negative numbers; including positive amounts (credits) would
  // inflate the total if we take absolute values across all transactions.
  const outflows = expenses.filter((e) => parseAmount(e.amount) < 0);

  if (!outflows.length) {
    return {
      totalExpenses: 0,
      averageExpense: 0,
      highestExpense: 0,
      totalTransactions: 0,
      categories: []
    };
  }

  const totalsByCategory = new Map<string, { total: number; count: number }>();
  let highestExpense = Number.NEGATIVE_INFINITY;
  let total = 0;

  outflows.forEach((expense) => {
    const numericAmount = parseAmount(expense.amount);
    // make the expense positive for display/aggregation (e.g. -12.50 -> 12.50)
    const absolute = Math.abs(numericAmount);
    total += absolute;
    highestExpense = Math.max(highestExpense, absolute);

    if (expense.category) {
      const current = totalsByCategory.get(expense.category) ?? { total: 0, count: 0 };
      current.total += absolute;
      current.count += 1;
      totalsByCategory.set(expense.category, current);
    }
  });

  const categories = Array.from(totalsByCategory.entries())
    .map(([name, value]) => ({ name, total: value.total, transactions: value.count }))
    .sort((a, b) => b.total - a.total);

  return {
    // Return positive totals (user-facing) and compute averages based on the
    // number of outflow transactions only.
    totalExpenses: Number(total.toFixed(2)),
    averageExpense: Number((total / outflows.length).toFixed(2)),
    highestExpense: Number((highestExpense > 0 ? highestExpense : 0).toFixed(2)),
    totalTransactions: outflows.length,
    categories
  };
};

export const formatCurrency = (value: number, currency: string = 'EUR') => {
  return new Intl.NumberFormat('it-IT', {
    style: 'currency',
    currency
  }).format(value);
};

export const formatDateTime = (value?: string) => {
  if (!value) {
    return '—';
  }
  try {
    return new Intl.DateTimeFormat('it-IT', {
      year: 'numeric',
      month: 'short',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    }).format(new Date(value));
  } catch (error) {
    return value;
  }
};

export const monthLabels = ['Gen', 'Feb', 'Mar', 'Apr', 'Mag', 'Giu', 'Lug', 'Ago', 'Set', 'Ott', 'Nov', 'Dic'];

export const buildMonthlySeries = (data: Record<string, number | string>, year: string) => {
  const series = monthLabels.map((label, index) => ({
    month: label,
    value: 0
  }));

  Object.entries(data).forEach(([key, value]) => {
    if (!key.startsWith(year)) {
      return;
    }
    const monthIndex = Number(key.split('-')[1]) - 1;
    if (monthIndex >= 0 && monthIndex < 12) {
      const numeric = Math.abs(parseAmount(value));
      series[monthIndex].value = Number(numeric.toFixed(2));
    }
  });

  return series;
};
