import axios from 'axios';

const api = axios.create({
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.message || err.message;
    console.error('API Error:', msg);
    return Promise.reject(err);
  },
);

export default api;

export const mcpApi = axios.create({
  baseURL: '/mcp-api',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
});
