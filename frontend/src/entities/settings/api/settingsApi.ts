import api from '@/shared/api/client';
import type { ApiResponse } from '@/shared/api/types';
import type { SystemSettings } from '../model/types';

const BASE = '/api/v1/settings';

export const settingsApi = {
  get: () => api.get<ApiResponse<SystemSettings>>(BASE).then((r) => r.data.data),
  update: (data: SystemSettings) => api.put<ApiResponse<SystemSettings>>(BASE, data).then((r) => r.data.data),
};
