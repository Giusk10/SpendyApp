export interface Expense {
  id?: string;
  type: string;
  product: string;
  startedDate?: string;
  completedDate?: string;
  description: string;
  amount: number;
  fee?: number;
  currency?: string;
  state?: string;
  category?: string;
}

export interface AggregatedExpenseMetrics {
  totalExpenses: number;
  averageExpense: number;
  highestExpense: number;
  totalTransactions: number;
  categories: Array<{
    name: string;
    total: number;
    transactions: number;
  }>;
}
