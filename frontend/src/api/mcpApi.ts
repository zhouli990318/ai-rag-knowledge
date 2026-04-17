import { mcpApi } from './client';
import { ApiResponse, McpApiSource, McpConnectionInfo, McpToolMapping } from './types';

const BASE = '/v1/mcp';

export const mcpGatewayApi = {
  listSources: () => mcpApi.get<ApiResponse<McpApiSource[]>>(`${BASE}/sources`).then((r) => r.data.data),
  getConnectionInfo: () => mcpApi.get<ApiResponse<McpConnectionInfo>>(`${BASE}/connection-info`).then((r) => r.data.data),
  getSourceConnectionInfo: (id: number) => mcpApi.get<ApiResponse<McpConnectionInfo>>(`${BASE}/sources/${id}/connection-info`).then((r) => r.data.data),
  getSource: (id: number) => mcpApi.get<ApiResponse<McpApiSource>>(`${BASE}/sources/${id}`).then((r) => r.data.data),
  createSource: (data: Record<string, unknown>) => mcpApi.post<ApiResponse<McpApiSource>>(`${BASE}/sources`, data).then((r) => r.data.data),
  updateSource: (id: number, data: Record<string, unknown>) => mcpApi.put<ApiResponse<McpApiSource>>(`${BASE}/sources/${id}`, data).then((r) => r.data.data),
  deleteSource: (id: number) => mcpApi.delete(`${BASE}/sources/${id}`),
  parseSpec: (id: number, data: { openApiSpec?: string; openApiUrl?: string }) =>
    mcpApi.post<ApiResponse<McpToolMapping[]>>(`${BASE}/sources/${id}/parse`, data).then((r) => r.data.data),
  getTools: (id: number) => mcpApi.get<ApiResponse<McpToolMapping[]>>(`${BASE}/sources/${id}/tools`).then((r) => r.data.data),
  updateTool: (id: number, data: Record<string, unknown>) => mcpApi.put<ApiResponse<McpToolMapping>>(`${BASE}/tools/${id}`, data).then((r) => r.data.data),
  testTool: (id: number, args: string) => mcpApi.post<ApiResponse<string>>(`${BASE}/tools/${id}/test`, { arguments: args }).then((r) => r.data.data),
};
