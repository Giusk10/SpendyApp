import { formClient, httpClient } from './httpClient';
import { Expense } from '../types/expense';

export interface DateRangePayload {
  startedDate: string;
  completedDate: string;
}

export interface MonthPayload {
  month: string;
  year: string;
}

export const uploadExpenseFile = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);

  const response = await formClient.post('/Expense/rest/expense/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });

  return response.data as string;
};

export const fetchExpenses = async () => {
  const response = await httpClient.get<Expense[]>('/Expense/rest/expense/getExpenses');
  return response.data;
};

export const fetchExpensesByDate = async (payload: DateRangePayload) => {
  const response = await httpClient.post<Expense[]>('/Expense/rest/expense/getExpenseByDate', payload);
  return response.data;
};

export const fetchExpensesByMonth = async (payload: MonthPayload) => {
  const response = await httpClient.post<Expense[]>('/Expense/rest/expense/getExpenseByMonth', payload);
  return response.data;
};

export const fetchMonthlyAmountOfYear = async (year: string) => {
  const response = await httpClient.post<Record<string, number | string>>(
    '/Expense/rest/expense/getMonthlyAmountOfYear',
    { year }
  );
  return response.data;
};
