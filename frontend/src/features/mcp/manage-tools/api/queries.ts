import { useQuery, useMutation } from '@tanstack/react-query';
import { mcpGatewayApi, toolIndexApi } from '@/entities/mcp';
import { useSnackbar } from 'notistack';

const triggerReindex = () => { toolIndexApi.reindex().catch(() => {}); };

export function useTools(sourceId: number | undefined) {
  return useQuery({
    queryKey: ['mcp-tools', sourceId],
    queryFn: () => mcpGatewayApi.getTools(sourceId!),
    enabled: !!sourceId,
  });
}

export function useParseSpec(sourceId: number) {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: (data: { openApiSpec?: string; openApiUrl?: string }) => mcpGatewayApi.parseSpec(sourceId, data),
    onSuccess: () => {
      enqueueSnackbar('解析完成', { variant: 'success' });
      triggerReindex();
    },
    onError: () => enqueueSnackbar('解析失败', { variant: 'error' }),
  });
}

export function useToggleTool() {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: ({ id, enabled }: { id: number; enabled: boolean }) => mcpGatewayApi.updateTool(id, { enabled }),
    onSuccess: () => {
      enqueueSnackbar('工具状态已更新', { variant: 'success' });
      triggerReindex();
    },
    onError: (e: any) => enqueueSnackbar(e?.response?.data?.message || '操作失败', { variant: 'error' }),
  });
}

export function useUpdateTool() {
  const { enqueueSnackbar } = useSnackbar();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Record<string, unknown> }) => mcpGatewayApi.updateTool(id, data),
    onSuccess: () => {
      enqueueSnackbar('已更新', { variant: 'success' });
      triggerReindex();
    },
    onError: () => enqueueSnackbar('更新失败', { variant: 'error' }),
  });
}

export function useTestTool() {
  return useMutation({
    mutationFn: (payload: { id: number; args: string }) => mcpGatewayApi.testTool(payload.id, payload.args),
  });
}
