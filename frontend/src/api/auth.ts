import { httpClient } from './httpClient';

export interface LoginPayload {
  username?: string;
  email?: string;
  password: string;
}

export interface RegisterPayload {
  username: string;
  password: string;
  name: string;
  surname: string;
  email: string;
}

export const login = async (payload: LoginPayload) => {
  const response = await httpClient.post<{ token: string }>('/Auth/auth/login', payload);
  return response.data;
};

export const register = async (payload: RegisterPayload) => {
  const response = await httpClient.post<string>('/Auth/auth/register', payload, {
    headers: { 'Content-Type': 'application/json' }
  });
  return response.data;
};

export const linkHouse = async (houseCode: string) => {
  const response = await httpClient.post('/Auth/auth/external/link-house', { houseCode });
  return response.data as { message: string };
};

export const getCoinquilini = async (houseId: string) => {
  const response = await httpClient.get('/Auth/client/retrieveCoinquy', {
    params: { houseId }
  });
  return response.data as Array<{
    id?: string;
    username?: string;
    name?: string;
    surname?: string;
    email?: string;
  }>;
};
