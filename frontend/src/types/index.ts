export interface Expense {
  id?: string;
  type: string;
  product: string;
  startedDate: string | null;
  completedDate: string | null;
  description: string;
  amount: number;
  fee: number;
  currency: string;
  state: string;
  category: string;
}

export interface MonthlyAmountResponse {
  [month: string]: number;
}

export interface ImportResponse {
  message: string;
}
