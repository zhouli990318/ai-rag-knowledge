export interface McpApiSource {
  id: number;
  name: string;
  description: string;
  baseUrl: string;
  authType: string;
  active: boolean;
  toolMappings: McpToolMapping[];
  healthStatus?: HealthStatus;
  lastHealthCheckAt?: string;
  lastHealthyAt?: string;
  consecutiveFailures?: number;
  lastErrorMessage?: string;
}

export type HealthStatus = 'UNKNOWN' | 'HEALTHY' | 'DEGRADED' | 'UNREACHABLE';

export interface SourceHealth {
  id: number;
  name: string;
  baseUrl: string;
  active: boolean;
  healthStatus: HealthStatus;
  lastHealthCheckAt?: string;
  lastHealthyAt?: string;
  consecutiveFailures: number;
  lastErrorMessage?: string;
}

export interface McpConnectionInfo {
  serverName: string;
  version: string;
  sseUrl: string;
  streamableHttpUrl: string;
}

export interface McpToolMapping {
  id: number;
  operationId: string;
  toolName: string;
  toolDescription: string;
  httpMethod: string;
  path: string;
  parameterSchema?: string;
  responseSchema?: string;
  examplePayload?: string;
  enabled: boolean;
}
