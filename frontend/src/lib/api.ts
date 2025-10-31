import axios from 'axios';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000
});

apiClient.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const stored = window.localStorage.getItem('spendyapp_auth');
    if (stored) {
      try {
        const { token } = JSON.parse(stored) as { token: string | null };
        if (token) {
          config.headers = config.headers ?? {};
          config.headers.Authorization = `Bearer ${token}`;
        }
      } catch (error) {
        console.warn('Unable to attach auth token', error);
      }
    }
  }
  return config;
});

export const routes = {
  auth: {
    login: '/Auth/auth/login',
    register: '/Auth/auth/register',
    linkHouse: '/Auth/auth/external/link-house'
  },
  expense: {
    import: '/Expense/rest/expense/import',
    list: '/Expense/rest/expense/getExpenses',
    byDate: '/Expense/rest/expense/getExpenseByDate',
    byMonth: '/Expense/rest/expense/getExpenseByMonth',
    monthlyAmount: '/Expense/rest/expense/getMonthlyAmountOfYear'
  }
} as const;
