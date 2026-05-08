import axios from 'axios';

const api = axios.create({
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' },
});

function formatError(err: any): string {
  if (err.response?.data?.message) return err.response.data.message;
  if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) return '请求超时，请稍后重试';
  if (!err.response) return '网络连接失败，请检查网络';
  if (err.response.status >= 500) return '服务器异常，请稍后重试';
  return err.message || '未知错误';
}

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = formatError(err);
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

mcpApi.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = formatError(err);
    console.error('MCP API Error:', msg);
    return Promise.reject(err);
  },
);
