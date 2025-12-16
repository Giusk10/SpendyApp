import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '';

export const httpClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export const formClient = axios.create({
  baseURL
});

export const setAuthToken = (token?: string) => {
  if (token) {
    httpClient.defaults.headers.common.Authorization = `Bearer ${token}`;
    formClient.defaults.headers.common.Authorization = `Bearer ${token}`;
  } else {
    delete httpClient.defaults.headers.common.Authorization;
    delete formClient.defaults.headers.common.Authorization;
  }
};

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      return Promise.reject({
        status: error.response.status,
        message: error.response.data?.message || error.response.data || error.message
      });
    }
    return Promise.reject({ status: 500, message: error.message });
  }
);

formClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      return Promise.reject({
        status: error.response.status,
        message: error.response.data?.message || error.response.data || error.message
      });
    }
    return Promise.reject({ status: 500, message: error.message });
  }
);
