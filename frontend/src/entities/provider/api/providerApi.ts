import api from '@/shared/api/client';
import type { ApiResponse } from '@/shared/api/types';
import type { Provider, ProviderType } from '../model/types';

const BASE = '/api/v1/providers';

export const providerApi = {
  list: () => api.get<ApiResponse<Provider[]>>(BASE).then((r) => r.data.data),
  create: (data: Record<string, unknown>) => api.post<ApiResponse<Provider>>(BASE, data).then((r) => r.data.data),
  update: (id: number, data: Record<string, unknown>) => api.put<ApiResponse<Provider>>(`${BASE}/${id}`, data).then((r) => r.data.data),
  delete: (id: number) => api.delete(`${BASE}/${id}`),
  types: () => api.get<ApiResponse<ProviderType[]>>(`${BASE}/types`).then((r) => r.data.data),
  test: (id: number) => api.post<ApiResponse<string>>(`${BASE}/${id}/test`).then((r) => r.data.data),
  toggle: (id: number) => api.put<ApiResponse<Provider>>(`${BASE}/${id}/toggle`).then((r) => r.data.data),
};
