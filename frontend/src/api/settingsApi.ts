import api from './client';
import { ApiResponse, SystemSettings } from './types';

const BASE = '/api/v1/settings';

export const settingsApi = {
  get: () => api.get<ApiResponse<SystemSettings>>(BASE).then((r) => r.data.data),
  update: (data: SystemSettings) => api.put<ApiResponse<SystemSettings>>(BASE, data).then((r) => r.data.data),
};
